package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.platform.dto.CreatePlatformUserRequest;
import mz.multicore.erp.modules.platform.dto.GrantAccessRequest;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;
import mz.multicore.erp.modules.platform.dto.PlatformUserDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Vista de gestão de utilizadores de todas as empresas clientes (superadmin).
 */
public class PlatformUsersView extends JPanel {

    private static final String[] TENANT_ROLES = {"EMPLOYEE", "SELLER", "MANAGER", "ADMIN"};

    private final PlatformApiClient platformApiClient;
    private final Supplier<List<PlatformCompanyDTO>> companiesSupplier;
    private final PlatformFeedbackHandler feedbackHandler;

    private DefaultTableModel usersModel;
    private JTable usersTable;
    private List<PlatformUserDTO> users = new ArrayList<>();

    public PlatformUsersView(PlatformApiClient platformApiClient,
                             Supplier<List<PlatformCompanyDTO>> companiesSupplier,
                             PlatformFeedbackHandler feedbackHandler) {
        this.platformApiClient = platformApiClient;
        this.companiesSupplier = companiesSupplier;
        this.feedbackHandler = feedbackHandler;

        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Utilizador");
        newBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        newBtn.addActionListener(e -> createPlatformUser());

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadUsers);
        ModernButton resetTwoFactorBtn = UIHelper.createIconButton("Repor dupla autenticação", "fas-mobile-alt");
        resetTwoFactorBtn.addActionListener(e -> {
            PlatformUserDTO user = selectedUser();
            if (user != null) PlatformTwoFactorAction.reset(this, platformApiClient, user.username());
        });
        mz.multicore.erp.gui.components.ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Editar Utilizador", UIHelper.icon("fas-pen", 14), this::editUser)
                .addAction("Conceder/Alterar Acesso", UIHelper.icon("fas-user-shield", 14, UIHelper.ACCENT_BLUE), this::grantAccess)
                .addAction("Revogar Acesso", UIHelper.icon("fas-user-slash", 14), this::revokeAccess)
                .addAction("Repor Senha", UIHelper.icon("fas-key", 14), this::resetPassword)
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), this::toggleUserActive);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Utilizador", "Nome", "Empresas & Papéis", "Estado"};
        usersModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = new JTable(usersModel);
        UIHelper.styleTable(usersTable);
        UIHelper.ensureHeadersFit(usersTable);
        usersTable.putClientProperty("noRowInspector", Boolean.TRUE);
        usersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editUser();
            }
        });
        JScrollPane scroll = new JScrollPane(usersTable);
        UIHelper.styleScrollPane(scroll);

        JTextField uSearch = TableFilter.searchField("Utilizador, nome ou empresa…");
        JComboBox<String> uEstado = TableFilter.combo("Todos os estados", "Activo", "Inactivo", "SuperAdmin");
        TableFilter.install(usersTable, uSearch, new TableFilter.ColumnFilter(uEstado, 3));
        JPanel uBar = TableFilter.bar(uSearch, TableFilter.label("Estado:"), uEstado);
        uBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        listCard.add(UIHelper.tableCardTop("Utilizadores de Todas as Empresas", uBar, refreshBtn, resetTwoFactorBtn, moreBtn, newBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);

        add(listCard, BorderLayout.CENTER);
    }

    public void loadUsers() {
        UIHelper.loadAsync(this, platformApiClient::listUsers, loaded -> {
            users = loaded;
            usersModel.setRowCount(0);
            for (PlatformUserDTO u : users) {
                usersModel.addRow(new Object[]{
                        u.username(), u.name(), describeAccesses(u),
                        u.platformAdmin() ? "SUPERADMIN" : (u.active() ? "ACTIVO" : "INATIVO")
                });
            }
        }, error -> feedbackHandler.showError("utilizadores", error, this::loadUsers));
    }

    private String describeAccesses(PlatformUserDTO u) {
        if (u.companies().isEmpty()) return u.platformAdmin() ? "(plataforma)" : "—";
        return u.companies().stream()
                .map(c -> c.companyName() + ": " + UIHelper.humanRole(c.role()))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private PlatformUserDTO selectedUser() {
        int row = TableFilter.selectedModelRow(usersTable);
        if (row < 0 || row >= users.size()) {
            feedbackHandler.showNotice(FeedbackType.WARNING, "Seleccione um utilizador", "Escolha um utilizador na lista para continuar.");
            return null;
        }
        return users.get(row);
    }

    private void editUser() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;

        JTextField nameField = new JTextField(user.name());
        UIHelper.styleTextField(nameField);
        JPanel form = UIHelper.createDialogForm("Nome completo:", nameField);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Editar Utilizador",
                "fas-pen", "Utilizador: " + user.username(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            if (nameField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("O nome é obrigatório.");
            }
            String name = nameField.getText().trim();
            return () -> platformApiClient.updateUser(user.username(), name);
        });
        if (dlg.showDialog()) {
            feedbackHandler.showSuccess("Utilizador actualizado.");
            loadUsers();
        }
    }

    private void createPlatformUser() {
        List<PlatformCompanyDTO> companies = companiesSupplier.get();
        if (companies == null || companies.isEmpty()) {
            feedbackHandler.showNotice(FeedbackType.WARNING, "Empresa necessária", "Crie primeiro uma empresa.");
            return;
        }
        JTextField usernameField = new JTextField();
        JTextField nameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<CompanyItem> companyCombo = companyCombo(companies);
        JComboBox<String> roleCombo = new JComboBox<>(TENANT_ROLES);
        UIHelper.styleTextField(usernameField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(passwordField);
        UIHelper.styleComboBox(companyCombo);
        UIHelper.styleComboBox(roleCombo);
        UIHelper.humanizeRoleCombo(roleCombo);

        JPanel form = UIHelper.createDialogForm(
                "Utilizador:", usernameField,
                "Nome completo:", nameField,
                "Senha:", passwordField,
                "Empresa:", companyCombo,
                "Papel:", roleCombo
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Novo Utilizador",
                "fas-user-plus", "Conta ligada a uma empresa", form).setConfirmButton("Criar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            if (usernameField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()
                    || passwordField.getPassword().length == 0) {
                throw new IllegalArgumentException("Utilizador, nome e senha são obrigatórios.");
            }
            CreatePlatformUserRequest request = new CreatePlatformUserRequest(
                    usernameField.getText().trim(), nameField.getText().trim(),
                    new String(passwordField.getPassword()),
                    ((CompanyItem) companyCombo.getSelectedItem()).id(),
                    (String) roleCombo.getSelectedItem());
            return () -> platformApiClient.createUser(request);
        });

        if (dlg.showDialog()) {
            feedbackHandler.showSuccess("Utilizador criado.");
            loadUsers();
        }
    }

    private void grantAccess() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        List<PlatformCompanyDTO> companies = companiesSupplier.get();
        if (companies == null || companies.isEmpty()) {
            feedbackHandler.showNotice(FeedbackType.WARNING, "Sem empresas", "Crie uma empresa antes de conceder acesso.");
            return;
        }
        JComboBox<CompanyItem> companyCombo = companyCombo(companies);
        JComboBox<String> roleCombo = new JComboBox<>(TENANT_ROLES);
        UIHelper.styleComboBox(companyCombo);
        UIHelper.styleComboBox(roleCombo);
        UIHelper.humanizeRoleCombo(roleCombo);

        JPanel form = UIHelper.createDialogForm("Empresa:", companyCombo, "Papel:", roleCombo);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Conceder/Alterar Acesso",
                "fas-user-shield", "Utilizador: " + user.username(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            GrantAccessRequest request = new GrantAccessRequest(((CompanyItem) companyCombo.getSelectedItem()).id(),
                    (String) roleCombo.getSelectedItem());
            return () -> platformApiClient.grantAccess(user.username(), request);
        });

        if (dlg.showDialog()) {
            feedbackHandler.showSuccess("Acesso actualizado.");
            loadUsers();
        }
    }

    private void revokeAccess() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        if (user.companies().isEmpty()) {
            feedbackHandler.showNotice(FeedbackType.INFO, "Sem acessos", "O utilizador não tem acessos a revogar.");
            return;
        }
        CompanyItem[] items = user.companies().stream()
                .map(c -> new CompanyItem(c.companyId(), c.companyName()))
                .toArray(CompanyItem[]::new);
        JComboBox<CompanyItem> combo = new JComboBox<>(items);
        UIHelper.styleComboBox(combo);

        JPanel form = UIHelper.createDialogForm("Empresa:", combo);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Revogar Acesso",
                "fas-user-slash", "Utilizador: " + user.username(), form).setConfirmButton("Revogar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            Long companyId = ((CompanyItem) combo.getSelectedItem()).id();
            return () -> {
                platformApiClient.revokeAccess(user.username(), companyId);
                return null;
            };
        });

        if (dlg.showDialog()) {
            feedbackHandler.showSuccess("Acesso revogado.");
            loadUsers();
        }
    }

    private void resetPassword() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        JPasswordField passwordField = new JPasswordField();
        UIHelper.styleTextField(passwordField);
        JPanel form = UIHelper.createDialogForm("Nova senha:", passwordField);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Repor Senha",
                "fas-key", "Utilizador: " + user.username(), form).setConfirmButton("Repor", "fas-check");
        dlg.setOnSaveAsync(() -> {
            if (passwordField.getPassword().length == 0) {
                throw new IllegalArgumentException("Indique a nova senha.");
            }
            String password = new String(passwordField.getPassword());
            return () -> {
                platformApiClient.resetPassword(user.username(), password);
                return null;
            };
        });
        if (dlg.showDialog()) {
            feedbackHandler.showSuccess("Senha reposta.");
        }
    }

    private void toggleUserActive() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        boolean newState = !user.active();
        UIHelper.runWithProgress(this, "A actualizar utilizador…",
                () -> platformApiClient.setUserActive(user.username(), newState), ignored -> loadUsers(),
                error -> feedbackHandler.showError("utilizador", error, this::loadUsers));
    }

    private JComboBox<CompanyItem> companyCombo(List<PlatformCompanyDTO> companies) {
        CompanyItem[] items = companies.stream()
                .map(c -> new CompanyItem(c.id(), c.name()))
                .toArray(CompanyItem[]::new);
        return new JComboBox<>(items);
    }

    private record CompanyItem(Long id, String name) {
        @Override public String toString() { return name; }
    }
}
