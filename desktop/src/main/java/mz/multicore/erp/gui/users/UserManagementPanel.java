package mz.multicore.erp.gui.users;

import mz.multicore.erp.desktop.client.UserApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.users.dto.AppUserDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel executivo de Gestão de Utilizadores, Matriz de Permissões & PINs de Autorização.
 * Integrado no painel de Configurações da Empresa.
 */
public class UserManagementPanel extends JPanel {

    private final UserApiClient userApiClient;

    private JTable usersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JLabel totalUsersKpi;
    private JLabel totalManagersKpi;
    private JLabel activeUsersKpi;
    private JLabel pinsConfiguredKpi;

    private List<AppUserDTO> loadedUsers = new ArrayList<>();

    public UserManagementPanel(UserApiClient userApiClient) {
        this.userApiClient = userApiClient;
        initUi();
        refreshDataAsync();
    }

    private void initUi() {
        setLayout(new BorderLayout(0, 14));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(16, 18, 16, 18));

        // 1. KPI Cards Superiores
        JPanel kpiGrid = KpiCard.createGrid(4);

        kpiGrid.add(KpiCard.createCard("Total Utilizadores", totalUsersKpi = new JLabel("0"), "Operadores registados", "fas-users", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Gestores / Admin", totalManagersKpi = new JLabel("0"), "Com alçadas elevadas", "fas-user-tie", UIHelper.ACCENT_CYAN));
        kpiGrid.add(KpiCard.createCard("Utilizadores Ativos", activeUsersKpi = new JLabel("0"), "Contas em operação", "fas-user-check", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("PINs de Gestor", pinsConfiguredKpi = new JLabel("0"), "Para autorização rápida", "fas-key", UIHelper.ACCENT_ORANGE));

        add(kpiGrid, BorderLayout.NORTH);

        // 2. Card Canónico com Tabela e Barra de Ferramentas
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Filtros (esquerda)
        searchField = TableFilter.searchField("Pesquisar utilizador, nome ou perfil…");
        JComboBox<String> estadoCombo = TableFilter.combo("Todos os estados", "ACTIVE", "SUSPENDED");

        // Acções (direita)
        ModernButton createBtn = UIHelper.createPrimaryButton("Novo Utilizador");
        createBtn.setIcon(UIHelper.icon("fas-user-plus", 14, Color.WHITE));
        createBtn.addActionListener(e -> new UserEditorDialog(this, userApiClient, null, this::refreshDataAsync).show());

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Operações")
                .addAction("Editar", UIHelper.icon("fas-user-edit", 14), this::openEditDialogForSelected)
                .addAction("PIN Gestor", UIHelper.icon("fas-key", 14), this::openSetPinDialogForSelected)
                .addAction("Redefinir Senha", UIHelper.icon("fas-lock", 14), this::openResetPasswordForSelected)
                .addAction("Ativar / Desativar", UIHelper.icon("fas-power-off", 14), this::toggleStatusForSelected);

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refreshDataAsync);

        JPanel filters = TableFilter.toolbar(
            new JComponent[]{searchField, TableFilter.label("Estado:", "fas-filter"), estadoCombo},
            null
        );
        card.add(UIHelper.tableCardTop("Gestão de Utilizadores", filters,
                actionsMenu, refreshBtn, createBtn), BorderLayout.NORTH);

        // Tabela
        String[] columns = new String[]{"ID", "Nome de Utilizador", "Nome Completo", "Perfil / Role", "Estado", "PIN Gestor"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        usersTable = new JTable(tableModel);
        UIHelper.styleTable(usersTable);
        ClientTablePagination.install(usersTable);
        RowDetailsInspector.install(usersTable);
        TableFilter.install(usersTable, searchField,
                new TableFilter.ColumnFilter(estadoCombo, 4));

        JScrollPane scroll = new JScrollPane(usersTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }


    public void refreshDataAsync() {
        if (GraphicsEnvironment.isHeadless() || "true".equalsIgnoreCase(System.getProperty("java.awt.headless"))
                || "true".equalsIgnoreCase(System.getProperty("multicore.test.headless"))) {
            return;
        }

        new SwingWorker<List<AppUserDTO>, Void>() {
            @Override
            protected List<AppUserDTO> doInBackground() throws Exception {
                if (userApiClient != null) {
                    return userApiClient.getAllUsers();
                }
                return List.of(new AppUserDTO(1L, "admin", "Administrador Principal", "ADMIN", true, true, "admin@empresa.co.mz"));
            }

            @Override
            protected void done() {
                try {
                    loadedUsers = get();
                    updateTableAndKpis(loadedUsers);
                } catch (Exception ex) {
                    showError("Erro ao carregar utilizadores: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void updateTableAndKpis(List<AppUserDTO> users) {
        tableModel.setRowCount(0);
        int managers = 0;
        int active = 0;
        int pins = 0;

        for (AppUserDTO u : users) {
            boolean isManager = "ADMIN".equalsIgnoreCase(u.role()) || "MANAGER".equalsIgnoreCase(u.role());
            if (isManager) managers++;
            if (u.active()) active++;
            if (u.hasManagerPin()) pins++;

            String statusStr = u.active() ? "ATIVO" : "INATIVO";
            String pinStr = u.hasManagerPin() ? "CONFIGURADO" : "—";

            tableModel.addRow(new Object[]{
                    u.id(),
                    u.username(),
                    u.name(),
                    u.role() != null ? u.role().toUpperCase() : "—",
                    statusStr,
                    pinStr
            });
        }

        totalUsersKpi.setText(String.valueOf(users.size()));
        totalManagersKpi.setText(String.valueOf(managers));
        activeUsersKpi.setText(String.valueOf(active));
        pinsConfiguredKpi.setText(String.valueOf(pins));
    }

    private AppUserDTO getSelectedUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0 || loadedUsers.isEmpty() || row >= loadedUsers.size()) {
            showWarning("Selecione um utilizador na tabela.");
            return null;
        }
        return loadedUsers.get(row);
    }

    private void openEditDialogForSelected() {
        AppUserDTO selected = getSelectedUser();
        if (selected != null) {
            new UserEditorDialog(this, userApiClient, selected, this::refreshDataAsync).show();
        }
    }

    private void openSetPinDialogForSelected() {
        AppUserDTO selected = getSelectedUser();
        if (selected == null) return;

        String pin = ModernMessageDialog.prompt(this, FeedbackType.INFO, "Definir PIN de Gestor",
                "Novo PIN de gestor de 4 dígitos para '" + selected.username() + "':", "", true);
        if (pin != null && !pin.isBlank()) {
            try {
                userApiClient.setManagerPin(selected.username(), pin.trim());
                showInfo("PIN de gestor atribuído com sucesso a " + selected.name());
                refreshDataAsync();
            } catch (Exception ex) {
                showError("Erro ao definir PIN: " + ex.getMessage());
            }
        }
    }

    private void openResetPasswordForSelected() {
        AppUserDTO selected = getSelectedUser();
        if (selected == null) return;

        String pass = ModernMessageDialog.prompt(this, FeedbackType.INFO, "Redefinir Senha",
                "Nova senha para '" + selected.username() + "':", "", true);
        if (pass != null && !pass.isBlank()) {
            try {
                userApiClient.resetPassword(selected.username(), pass.trim());
                showInfo("Senha redefinida com sucesso para " + selected.name());
            } catch (Exception ex) {
                showError("Erro ao redefinir senha: " + ex.getMessage());
            }
        }
    }

    private void toggleStatusForSelected() {
        AppUserDTO selected = getSelectedUser();
        if (selected == null) return;

        boolean newStatus = !selected.active();
        String actionStr = newStatus ? "ativar" : "desativar";

        boolean confirmed = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(this),
                FeedbackType.WARNING, "Confirmar Alteração de Estado",
                "Deseja realmente " + actionStr + " o utilizador '" + selected.username() + "'?", "Confirmar");
        if (confirmed) {
            try {
                userApiClient.toggleStatus(selected.username(), newStatus);
                showInfo("Estado do utilizador alterado com sucesso.");
                refreshDataAsync();
            } catch (Exception ex) {
                showError("Erro ao alterar estado: " + ex.getMessage());
            }
        }
    }

    private void showError(String message) {
        ToastManager.show(this, FeedbackType.ERROR, message);
    }

    private void showWarning(String message) {
        ToastManager.show(this, FeedbackType.WARNING, message);
    }

    private void showInfo(String message) {
        ToastManager.success(this, message);
    }
}
