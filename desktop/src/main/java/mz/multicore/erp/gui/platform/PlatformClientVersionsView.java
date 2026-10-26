package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.TreeSet;

/**
 * Vista de auditoria de versões dos clientes desktop em uso por cada empresa.
 */
public class PlatformClientVersionsView extends JPanel {

    private final PlatformApiClient platformApiClient;
    private final PlatformFeedbackHandler feedbackHandler;

    private DefaultTableModel versionsModel;
    private JTable versionsTable;
    private JLabel versionsSummary;

    public PlatformClientVersionsView(PlatformApiClient platformApiClient, PlatformFeedbackHandler feedbackHandler) {
        this.platformApiClient = platformApiClient;
        this.feedbackHandler = feedbackHandler;

        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> loadClientVersions());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Empresa", "Versão", "Último acesso", "Utilizador", "Visto pela 1.ª vez"};
        versionsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        versionsTable = new JTable(versionsModel);
        UIHelper.styleTable(versionsTable);
        UIHelper.ensureHeadersFit(versionsTable);
        JScrollPane scroll = new JScrollPane(versionsTable);
        UIHelper.styleScrollPane(scroll);

        JTextField search = TableFilter.searchField("Empresa, versão ou utilizador…");
        TableFilter.install(versionsTable, search);
        JPanel bar = TableFilter.bar(search);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        card.add(UIHelper.tableCardTop("Versões dos Clientes — quem está em quê", bar, refresh), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        versionsSummary = new JLabel(" ");
        versionsSummary.setForeground(UIHelper.TEXT_LIGHT);
        versionsSummary.setBorder(new EmptyBorder(10, 2, 0, 2));
        card.add(versionsSummary, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    public void loadClientVersions() {
        if (versionsModel == null) return;
        UIHelper.loadAsync(this, platformApiClient::listClientVersions, loaded -> {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            versionsModel.setRowCount(0);
            Set<String> distinctVersions = new TreeSet<>();
            for (var usage : loaded) {
                distinctVersions.add(usage.clientVersion());
                versionsModel.addRow(new Object[]{
                        usage.companyName(),
                        usage.clientVersion(),
                        usage.lastSeenAt() == null ? "—" : usage.lastSeenAt().format(fmt),
                        usage.lastUsername() == null ? "—" : usage.lastUsername(),
                        usage.firstSeenAt() == null ? "—" : usage.firstSeenAt().format(fmt)
                });
            }
            versionsSummary.setText(loaded.isEmpty()
                    ? "Ainda não foi registada nenhuma versão. Os clientes identificam-se ao usar o sistema."
                    : String.format("<html><b>%d</b> registo(s) · <b>%d</b> versão(ões) diferente(s) em uso: %s</html>",
                            loaded.size(), distinctVersions.size(), String.join(", ", distinctVersions)));
        }, error -> feedbackHandler.showError("versões dos clientes", error, this::loadClientVersions));
    }
}
