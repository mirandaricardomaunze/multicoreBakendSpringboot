package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.CostCenterDTO;
import mz.multicore.erp.modules.accounting.model.CostCenterType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Configurações › Centros de Custo (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1, §3.3): the cost centres of the
 * active company, the manual ones created/renamed/deactivated/deleted here, and the choice of the default centre.
 * Warehouse centres follow their warehouse and are read-only. Managing is ADMIN/MANAGER; the backend decides,
 * this only disables what the server would refuse.
 */
public class CostCentersPanel extends JPanel {

    private final AccountingApiClient accountingApiClient;
    private final DefaultTableModel centersModel = new DefaultTableModel(
            new String[]{"Código", "Nome", "Tipo", "Estado", "Por omissão"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable centersTable = new JTable(centersModel);
    private final List<CostCenterDTO> centers = new ArrayList<>();
    private final JLabel defaultCenterLabel = new JLabel();
    private final ModernButton newBtn = UIHelper.createSuccessButton("Novo centro");
    private final ModernButton editBtn = UIHelper.createSecondaryButton("Editar");
    private final ModernButton defaultBtn = UIHelper.createSecondaryButton("Tornar por omissão");
    private final ModernButton deleteBtn = UIHelper.createDangerButton("Apagar");

    public CostCentersPanel(AccountingApiClient accountingApiClient) {
        this.accountingApiClient = accountingApiClient;
        setLayout(new BorderLayout(0, Spacing.LG));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(Spacing.LG, Spacing.LG, Spacing.LG, Spacing.LG));
        add(buildCentersCard(), BorderLayout.CENTER);
        centersTable.getSelectionModel().addListSelectionListener(e -> updateActions());
        updateActions();
    }

    private JComponent buildCentersCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, Spacing.SM));
        card.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));

        UIHelper.styleTable(centersTable);
        UIHelper.ensureHeadersFit(centersTable);
        centersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(centersTable);
        UIHelper.styleScrollPane(scroll);

        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> openForm(null));
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> withSelected(this::openForm));
        defaultBtn.setIcon(UIHelper.icon("fas-star", 14));
        defaultBtn.setToolTipText("Centro usado quando o lançamento não tem armazém nem centro escolhido");
        defaultBtn.addActionListener(e -> withSelected(this::makeDefault));
        deleteBtn.setIcon(UIHelper.icon("fas-trash-alt", 14));
        deleteBtn.addActionListener(e -> withSelected(this::delete));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);

        defaultCenterLabel.setFont(Typography.CAPTION);
        defaultCenterLabel.setForeground(UIHelper.TEXT_MUTED);

        card.add(UIHelper.tableCardTop("Centros de custo da empresa", defaultCenterLabel,
                newBtn, editBtn, defaultBtn, deleteBtn, refreshBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("Os centros de armazém são criados com o armazém e acompanham o seu nome e estado. "
                + "Um centro com lançamentos não se apaga: desactive-o.");
        hint.setFont(Typography.CAPTION);
        hint.setForeground(UIHelper.TEXT_MUTED);
        card.add(hint, BorderLayout.SOUTH);
        return card;
    }

    public void refresh() {
        UIHelper.loadAsync(this, accountingApiClient::listCostCenters, this::applyCenters,
                error -> showError("carregar os centros de custo", error));
    }

    void applyCenters(List<CostCenterDTO> list) {
        centers.clear();
        if (list != null) centers.addAll(list);
        centersModel.setRowCount(0);
        String defaultText = "Centro por omissão: nenhum definido.";
        for (CostCenterDTO center : centers) {
            centersModel.addRow(new Object[]{center.code(), center.name(),
                    center.type() == CostCenterType.WAREHOUSE ? "Armazém" : "Manual",
                    center.active() ? "Activo" : "Inactivo",
                    center.defaultCenter() ? "Sim" : ""});
            if (center.defaultCenter()) {
                defaultText = "Centro por omissão: " + center.code() + " — " + center.name()
                        + " (lançamentos sem armazém nem centro escolhido).";
            }
        }
        defaultCenterLabel.setText(defaultText);
        updateActions();
    }

    private void updateActions() {
        boolean manager = SignedInUser.isManagerOrAdmin();
        CostCenterDTO selected = selected();
        boolean manual = selected != null && selected.type() == CostCenterType.MANUAL;
        newBtn.setEnabled(manager);
        editBtn.setEnabled(manager && manual);
        deleteBtn.setEnabled(manager && manual && !selected.defaultCenter());
        defaultBtn.setEnabled(manager && selected != null && selected.active() && !selected.defaultCenter());
    }

    private CostCenterDTO selected() {
        int row = centersTable.getSelectedRow();
        return row < 0 ? null : centers.get(centersTable.convertRowIndexToModel(row));
    }

    private void withSelected(java.util.function.Consumer<CostCenterDTO> action) {
        CostCenterDTO center = selected();
        if (center == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione o centro de custo.");
            return;
        }
        action.accept(center);
    }

    private void openForm(CostCenterDTO existing) {
        if (new CostCenterFormDialog(accountingApiClient, existing).show()) {
            refresh();
        }
    }

    private void makeDefault(CostCenterDTO center) {
        UIHelper.loadAsync(this, () -> accountingApiClient.setDefaultCostCenter(center.id()), dto -> {
            ToastManager.show(this, FeedbackType.SUCCESS, "Centro por omissão: " + dto.code() + " — " + dto.name() + ".");
            refresh();
        }, error -> showError("mudar o centro por omissão", error));
    }

    private void delete(CostCenterDTO center) {
        if (!UIHelper.confirm(this, "Apagar o centro " + center.code() + " — " + center.name() + "?\n"
                + "Só é possível se o centro não tiver lançamentos.", "Apagar centro de custo")) {
            return;
        }
        UIHelper.loadAsync(this, () -> {
            accountingApiClient.deleteCostCenter(center.id());
            return center;
        }, deleted -> {
            ToastManager.show(this, FeedbackType.SUCCESS, "Centro " + deleted.code() + " apagado.");
            refresh();
        }, error -> showError("apagar o centro de custo", error));
    }

    private void showError(String action, Throwable error) {
        ToastManager.show(this, FeedbackType.ERROR, "Não foi possível " + action + ": " + error.getMessage());
    }

    JTable centersTable() { return centersTable; }
    JLabel defaultCenterLabel() { return defaultCenterLabel; }
    ModernButton newButton() { return newBtn; }
    ModernButton editButton() { return editBtn; }
    ModernButton defaultButton() { return defaultBtn; }
    ModernButton deleteButton() { return deleteBtn; }
}
