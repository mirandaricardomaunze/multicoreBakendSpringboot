package mz.multicore.erp.gui.users;

import mz.multicore.erp.desktop.client.UserApiClient;
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

        searchField = TableFilter.searchField("Pesquisar utilizador, nome ou perfil…");
        JComboBox<String> estadoCombo = TableFilter.combo("Todos os estados", "Activo", "Suspenso");

        ModernButton createBtn = UIHelper.createSuccessButton("Novo Utilizador");
        createBtn.setIcon(UIHelper.icon("fas-user-plus", 14, Color.WHITE));
        createBtn.addActionListener(e -> new UserEditorDialog(this, userApiClient, null, this::refreshDataAsync).show());

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Gerir Utilizador")
                .addAction("Editar", UIHelper.icon("fas-user-edit", 14), () -> UserOperationsCoordinator.openEditDialog(this, userApiClient, getSelectedUser(), this::refreshDataAsync))
                .addAction("PIN Gestor", UIHelper.icon("fas-key", 14), () -> UserOperationsCoordinator.openSetPinDialog(this, userApiClient, getSelectedUser(), this::refreshDataAsync))
                .addAction("Redefinir Senha", UIHelper.icon("fas-lock", 14), () -> UserOperationsCoordinator.openResetPassword(this, userApiClient, getSelectedUser(), this::refreshDataAsync))
                .addAction("Ativar / Desativar", UIHelper.icon("fas-power-off", 14), () -> UserOperationsCoordinator.toggleStatus(this, userApiClient, getSelectedUser(), this::refreshDataAsync));

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refreshDataAsync);

        JPanel filters = TableFilter.toolbar(
                new JComponent[]{searchField, TableFilter.label("Estado:", "fas-filter"), estadoCombo},
                null
        );
        card.add(UIHelper.tableCardTop("Gestão de Utilizadores", filters,
                actionsMenu, refreshBtn, createBtn), BorderLayout.NORTH);

        String[] columns = new String[]{"ID", "Nome de Utilizador", "Nome Completo", "Perfil / Role", "Estado", "PIN Gestor"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        usersTable = new JTable(tableModel);
        UIHelper.styleTable(usersTable);
        UIHelper.ensureHeadersFit(usersTable);
        ClientTablePagination.install(usersTable);
        RowDetailsInspector.install(usersTable);
        TableFilter.install(usersTable, searchField, new TableFilter.ColumnFilter(estadoCombo, 4));

        TableQuickPeekController.install(usersTable, card, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            UserOperationsCoordinator.populateQuickPeek(peek, tableModel, row);
        });

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
            protected List<AppUserDTO> doInBackground() {
                if (userApiClient != null) {
                    return userApiClient.getAllUsers();
                }
                return List.of(new AppUserDTO(1L, "admin", "Administrador Principal", "ADMIN", true, true, "admin@empresa.co.mz"));
            }

            @Override
            protected void done() {
                try {
                    loadedUsers = get();
                    UserOperationsCoordinator.updateTableAndKpis(tableModel, loadedUsers, totalUsersKpi, totalManagersKpi, activeUsersKpi, pinsConfiguredKpi);
                } catch (Exception ex) {
                    ToastManager.show(UserManagementPanel.this, FeedbackType.ERROR, "Erro ao carregar utilizadores: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private AppUserDTO getSelectedUser() {
        return UserOperationsCoordinator.getSelectedUser(usersTable, loadedUsers, this);
    }
}
