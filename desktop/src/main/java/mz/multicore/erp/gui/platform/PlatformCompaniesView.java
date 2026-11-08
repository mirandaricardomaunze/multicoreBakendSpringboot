package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.platform.dto.CompanyProfile;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vista de gestão de empresas na consola da plataforma (superadmin).
 */
public class PlatformCompaniesView extends JPanel {

    private final PlatformApiClient platformApiClient;
    private final PlatformFeedbackHandler feedbackHandler;

    private DefaultTableModel companiesModel;
    private JTable companiesTable;
    private List<PlatformCompanyDTO> companies = new ArrayList<>();

    public PlatformCompaniesView(PlatformApiClient platformApiClient, PlatformFeedbackHandler feedbackHandler) {
        this.platformApiClient = platformApiClient;
        this.feedbackHandler = feedbackHandler;

        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton newBtn = UIHelper.createSuccessButton("Registar Empresa");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> createCompany());

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadCompanies);
        mz.multicore.erp.gui.components.ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Editar Empresa", UIHelper.icon("fas-pen", 14), this::editSelectedCompany)
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), this::toggleSelectedCompany);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Empresa", "NUIT", "Localidade", "Telefone", "Email", "Utilizadores", "Estado"};
        companiesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        companiesTable = new JTable(companiesModel);
        UIHelper.styleTable(companiesTable);
        UIHelper.ensureHeadersFit(companiesTable);
        companiesTable.putClientProperty("noRowInspector", Boolean.TRUE);
        companiesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedCompany();
            }
        });
        JScrollPane scroll = new JScrollPane(companiesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField cSearch = TableFilter.searchField("Nome, NUIT, cidade ou email…");
        JComboBox<String> cEstado = TableFilter.combo("Todos os estados", "Activa", "Suspensa");
        TableFilter.install(companiesTable, cSearch, new TableFilter.ColumnFilter(cEstado, 6));
        JPanel cBar = TableFilter.bar(cSearch, TableFilter.label("Estado:"), cEstado);
        cBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        listCard.add(UIHelper.tableCardTop("Empresas da Plataforma", cBar, refreshBtn, moreBtn, newBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);

        add(listCard, BorderLayout.CENTER);
    }

    public List<PlatformCompanyDTO> getCompanies() {
        return Collections.unmodifiableList(companies);
    }

    public void loadCompanies() {
        UIHelper.loadAsync(this, platformApiClient::listCompanies, loaded -> {
            companies = loaded;
            companiesModel.setRowCount(0);
            for (PlatformCompanyDTO c : companies) {
                companiesModel.addRow(new Object[]{
                        displayName(c), c.taxId(), locality(c.profile()),
                        c.phone() == null ? "" : c.phone(), c.email() == null ? "" : c.email(),
                        c.userCount(), c.active() ? "ACTIVA" : "SUSPENSA"
                });
            }
        }, error -> feedbackHandler.showError("empresas", error, this::loadCompanies));
    }

    private PlatformCompanyDTO selectedCompany() {
        int row = TableFilter.selectedModelRow(companiesTable);
        if (row < 0 || row >= companies.size()) {
            feedbackHandler.showNotice(FeedbackType.WARNING, "Seleccione uma empresa", "Escolha uma empresa na lista para continuar.");
            return null;
        }
        return companies.get(row);
    }

    private void createCompany() {
        CompanyWizardDialog.Outcome outcome =
                CompanyWizardDialog.showCreate(SwingUtilities.getWindowAncestor(this), platformApiClient);
        if (outcome == null) return;
        if (outcome.logoFailed()) {
            feedbackHandler.showNotice(FeedbackType.WARNING, "Empresa registada sem logótipo",
                    "A empresa, o administrador e a assinatura foram criados, mas o logótipo não foi carregado. "
                            + "Edite a empresa para tentar de novo.");
        } else {
            feedbackHandler.showSuccess("Empresa '" + displayName(outcome.company())
                    + "' registada com administrador e assinatura.");
        }
        loadCompanies();
    }

    private void editSelectedCompany() {
        PlatformCompanyDTO company = selectedCompany();
        if (company == null) return;
        CompanyWizardDialog.Outcome outcome =
                CompanyWizardDialog.showEdit(SwingUtilities.getWindowAncestor(this), platformApiClient, company);
        if (outcome == null) return;
        if (outcome.logoFailed()) {
            feedbackHandler.showNotice(FeedbackType.WARNING, "Logótipo não carregado",
                    "Os dados da empresa foram guardados, mas o logótipo não foi carregado. Tente de novo.");
        } else {
            feedbackHandler.showSuccess("Empresa actualizada.");
        }
        loadCompanies();
    }

    private static String displayName(PlatformCompanyDTO c) {
        CompanyProfile p = c.profile();
        return p != null && p.tradeName() != null && !p.tradeName().isBlank() ? p.tradeName() : c.name();
    }

    private static String locality(CompanyProfile p) {
        if (p == null) return "";
        String city = p.city() == null ? "" : p.city().trim();
        String province = p.province() == null ? "" : p.province().label();
        if (city.isEmpty()) return province;
        if (province.isEmpty() || province.toLowerCase().contains(city.toLowerCase())) return city;
        return city + ", " + province;
    }

    private void toggleSelectedCompany() {
        PlatformCompanyDTO company = selectedCompany();
        if (company == null) return;

        boolean newState = !company.active();
        String verb = newState ? "activar" : "suspender";
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(this),
                newState ? FeedbackType.INFO : FeedbackType.WARNING,
                newState ? "Activar empresa" : "Suspender empresa",
                "Deseja " + verb + " a empresa '" + company.name() + "'?"
                        + (newState ? "" : "\nOs utilizadores desta empresa deixam de poder iniciar sessão."),
                newState ? "Activar" : "Suspender")) return;

        UIHelper.runWithProgress(this, "A actualizar estado da empresa…",
                () -> platformApiClient.setCompanyActive(company.id(), newState), ignored -> loadCompanies(),
                error -> feedbackHandler.showError("estado da empresa", error, this::loadCompanies));
    }
}
