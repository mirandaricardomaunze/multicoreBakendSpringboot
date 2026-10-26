package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.platform.dto.CreateCompanyRequest;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;
import mz.multicore.erp.modules.platform.dto.UpdateCompanyRequest;

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

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Empresa");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> createCompany());

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadCompanies);
        mz.multicore.erp.gui.components.ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Editar Empresa", UIHelper.icon("fas-pen", 14), this::editSelectedCompany)
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), this::toggleSelectedCompany);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nome", "NUIT", "Email", "Nº Utilizadores", "Estado"};
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

        JTextField cSearch = TableFilter.searchField("Nome, NUIT ou email…");
        JComboBox<String> cEstado = TableFilter.combo("Todos os estados", "Activa", "Suspensa");
        TableFilter.install(companiesTable, cSearch, new TableFilter.ColumnFilter(cEstado, 4));
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
                        c.name(), c.taxId(), c.email() == null ? "" : c.email(),
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
        JTextField nameField = new JTextField();
        JTextField taxIdField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField phoneField = new JTextField();
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(taxIdField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);
        UIHelper.styleTextField(phoneField);
        final byte[][] logoHolder = {null};

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "NUIT:", taxIdField,
                "Email:", emailField,
                "Endereço:", addressField,
                "Telefone:", phoneField,
                "Logótipo:", logoPicker(logoHolder, false)
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Nova Empresa",
                "fas-building", "Registar uma empresa na plataforma", form)
                .setConfirmButton("Criar", "fas-plus");
        dlg.setOnSaveAsync(() -> {
            if (nameField.getText().trim().isEmpty() || taxIdField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Nome e NUIT são obrigatórios.");
            }
            CreateCompanyRequest request = new CreateCompanyRequest(
                    nameField.getText().trim(), taxIdField.getText().trim(),
                    emailField.getText().trim(), addressField.getText().trim(), phoneField.getText().trim());
            byte[] logo = logoHolder[0];
            return () -> {
                PlatformCompanyDTO created = platformApiClient.createCompany(request);
                if (logo != null) platformApiClient.updateCompanyLogo(created.id(), logo);
                return created;
            };
        });

        if (dlg.showDialog()) {
            feedbackHandler.showSuccess("Empresa criada com sucesso.");
            loadCompanies();
        }
    }

    private void editSelectedCompany() {
        PlatformCompanyDTO company = selectedCompany();
        if (company == null) return;

        JTextField nameField = new JTextField(company.name());
        JTextField emailField = new JTextField(company.email() == null ? "" : company.email());
        JTextField addressField = new JTextField(company.address() == null ? "" : company.address());
        JTextField phoneField = new JTextField(company.phone() == null ? "" : company.phone());
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);
        UIHelper.styleTextField(phoneField);
        final byte[][] logoHolder = {null};

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "Email:", emailField,
                "Endereço:", addressField,
                "Telefone:", phoneField,
                "Logótipo:", logoPicker(logoHolder, company.hasLogo())
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Editar Empresa",
                "fas-pen", "NUIT: " + company.taxId(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            if (nameField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("O nome é obrigatório.");
            }
            UpdateCompanyRequest request = new UpdateCompanyRequest(
                    nameField.getText().trim(), emailField.getText().trim(),
                    addressField.getText().trim(), phoneField.getText().trim());
            byte[] logo = logoHolder[0];
            return () -> {
                PlatformCompanyDTO updated = platformApiClient.updateCompany(company.id(), request);
                if (logo != null) platformApiClient.updateCompanyLogo(company.id(), logo);
                return updated;
            };
        });

        if (dlg.showDialog()) {
            feedbackHandler.showSuccess("Empresa actualizada.");
            loadCompanies();
        }
    }

    private JComponent logoPicker(byte[][] holder, boolean hasExisting) {
        JLabel preview = new JLabel(hasExisting ? "(logótipo actual definido)" : "(sem logótipo)");
        JButton pick = new JButton("Escolher Imagem", UIHelper.icon("fas-image", 16));
        pick.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagens (PNG, JPG)", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                byte[] bytes = UIHelper.readScaledImage(fc.getSelectedFile(), 320);
                if (bytes == null) {
                    feedbackHandler.showNotice(FeedbackType.WARNING, "Imagem inválida", "Seleccione uma imagem válida para o logótipo.");
                    return;
                }
                holder[0] = bytes;
                preview.setText(null);
                preview.setIcon(UIHelper.imageIconFromBytes(bytes, 64, 40));
            }
        });
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.add(pick);
        p.add(preview);
        return p;
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
