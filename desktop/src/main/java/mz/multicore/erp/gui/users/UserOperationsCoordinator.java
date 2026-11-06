package mz.multicore.erp.gui.users;

import mz.multicore.erp.desktop.client.UserApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.users.dto.AppUserDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordenador de operações, credenciais, PINs de autorização e visualização de utilizadores.
 */
public final class UserOperationsCoordinator {

    private UserOperationsCoordinator() {}

    public static void updateTableAndKpis(DefaultTableModel tableModel, List<AppUserDTO> users,
                                          JLabel totalUsersKpi, JLabel totalManagersKpi,
                                          JLabel activeUsersKpi, JLabel pinsConfiguredKpi) {
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

    public static void populateQuickPeek(QuickPeekPanel peek, DefaultTableModel tableModel, int row) {
        peek.setHeaderIcon("fas-user-shield", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(tableModel.getValueAt(row, 2)));
        peek.setSubtitle("Utilizador: " + tableModel.getValueAt(row, 1) + " • ID " + tableModel.getValueAt(row, 0));
        peek.setStatus(String.valueOf(tableModel.getValueAt(row, 4)), UIHelper.ACCENT_BLUE);
        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Nome de Utilizador", peek.cell(tableModel.getValueAt(row, 1), 1), false));
        items.add(new QuickPeekPanel.PeekItem("Perfil / Role", peek.cell(tableModel.getValueAt(row, 3), 3), true));
        items.add(new QuickPeekPanel.PeekItem("Estado", peek.cell(tableModel.getValueAt(row, 4), 4), false));
        items.add(new QuickPeekPanel.PeekItem("PIN de Gestor", peek.cell(tableModel.getValueAt(row, 5), 5), false));
        peek.setItems(items);
    }

    public static AppUserDTO getSelectedUser(JTable usersTable, List<AppUserDTO> loadedUsers, Component parent) {
        int row = usersTable.getSelectedRow();
        if (row < 0 || loadedUsers.isEmpty() || row >= loadedUsers.size()) {
            ToastManager.show(parent, FeedbackType.WARNING, "Selecione um utilizador na tabela.");
            return null;
        }
        return loadedUsers.get(row);
    }

    public static void openEditDialog(Component parent, UserApiClient client, AppUserDTO selected, Runnable onDone) {
        if (selected != null) {
            new UserEditorDialog(parent, client, selected, onDone).show();
        }
    }

    public static void openSetPinDialog(Component parent, UserApiClient client, AppUserDTO selected, Runnable onDone) {
        if (selected == null) return;
        String pin = ModernMessageDialog.prompt(parent, FeedbackType.INFO, "Definir PIN de Gestor",
                "Novo PIN de gestor de 4 dígitos para '" + selected.username() + "':", "", true);
        if (pin != null && !pin.isBlank()) {
            try {
                client.setManagerPin(selected.username(), pin.trim());
                ToastManager.success(parent, "PIN de gestor atribuído com sucesso a " + selected.name());
                onDone.run();
            } catch (Exception ex) {
                ToastManager.show(parent, FeedbackType.ERROR, "Erro ao definir PIN: " + ex.getMessage());
            }
        }
    }

    public static void openResetPassword(Component parent, UserApiClient client, AppUserDTO selected, Runnable onDone) {
        if (selected == null) return;
        String pass = ModernMessageDialog.prompt(parent, FeedbackType.INFO, "Redefinir Senha",
                "Nova senha para '" + selected.username() + "':", "", true);
        if (pass != null && !pass.isBlank()) {
            try {
                client.resetPassword(selected.username(), pass.trim());
                ToastManager.success(parent, "Senha redefinida com sucesso para " + selected.name());
                onDone.run();
            } catch (Exception ex) {
                ToastManager.show(parent, FeedbackType.ERROR, "Erro ao redefinir senha: " + ex.getMessage());
            }
        }
    }

    public static void toggleStatus(Component parent, UserApiClient client, AppUserDTO selected, Runnable onDone) {
        if (selected == null) return;
        boolean newStatus = !selected.active();
        String actionStr = newStatus ? "ativar" : "desativar";

        boolean confirmed = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(parent),
                FeedbackType.WARNING, "Confirmar Alteração de Estado",
                "Deseja realmente " + actionStr + " o utilizador '" + selected.username() + "'?", "Confirmar");
        if (confirmed) {
            try {
                client.toggleStatus(selected.username(), newStatus);
                ToastManager.success(parent, "Estado do utilizador alterado com sucesso.");
                onDone.run();
            } catch (Exception ex) {
                ToastManager.show(parent, FeedbackType.ERROR, "Erro ao alterar estado: " + ex.getMessage());
            }
        }
    }
}
