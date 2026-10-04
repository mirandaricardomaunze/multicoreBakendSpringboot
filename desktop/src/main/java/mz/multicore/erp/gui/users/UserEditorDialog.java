package mz.multicore.erp.gui.users;

import mz.multicore.erp.desktop.client.UserApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.users.dto.AppUserDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Diálogo modal de criação e edição de utilizadores e atribuição de perfis de acesso.
 */
public class UserEditorDialog {

    private final Component parent;
    private final UserApiClient userApiClient;
    private final AppUserDTO existingUser;
    private final Runnable onSuccess;

    public UserEditorDialog(Component parent, UserApiClient userApiClient, AppUserDTO existingUser, Runnable onSuccess) {
        this.parent = parent;
        this.userApiClient = userApiClient;
        this.existingUser = existingUser;
        this.onSuccess = onSuccess;
    }

    public void show() {
        boolean isEdit = existingUser != null;
        String title = isEdit ? "Editar Utilizador — " + existingUser.username() : "Novo Utilizador da Empresa";
        String icon = isEdit ? "fas-user-edit" : "fas-user-plus";

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // 1. Username
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        form.add(createLabel("Nome de Utilizador (Username):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        JTextField usernameField = new JTextField(isEdit ? existingUser.username() : "");
        usernameField.setEditable(!isEdit);
        UIHelper.styleTextField(usernameField);
        form.add(usernameField, gbc);

        // 2. Nome Completo
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        form.add(createLabel("Nome Completo / Operador:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        JTextField nameField = new JTextField(isEdit ? existingUser.name() : "");
        UIHelper.styleTextField(nameField);
        form.add(nameField, gbc);

        // 3. Senha (Apenas na Criação)
        JPasswordField passwordField = new JPasswordField();
        if (!isEdit) {
            gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
            form.add(createLabel("Senha Inicial:"), gbc);
            gbc.gridx = 1; gbc.weightx = 0.7;
            UIHelper.styleTextField(passwordField);
            form.add(passwordField, gbc);
        }

        // 4. Perfil / Role
        gbc.gridx = 0; gbc.gridy = isEdit ? 2 : 3; gbc.weightx = 0.3;
        form.add(createLabel("Perfil de Acesso (Role):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{
                "SELLER", "MANAGER", "ADMIN", "ACCOUNTANT", "HR_MANAGER"
        });
        UIHelper.styleComboBox(roleCombo);
        if (isEdit && existingUser.role() != null) {
            roleCombo.setSelectedItem(existingUser.role().toUpperCase());
        }
        form.add(roleCombo, gbc);

        Window win = parent instanceof Window ? (Window) parent : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        ModernFormDialog dialog = new ModernFormDialog(win, title, icon, "Preencha os dados do utilizador", form);
        dialog.setOnSaveAsync(() -> {
            String username = usernameField.getText().trim();
            String name = nameField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();
            String pass = !isEdit ? new String(passwordField.getPassword()).trim() : null;

            if (username.isBlank() || name.isBlank()) {
                throw new IllegalArgumentException("Username e Nome Completo são obrigatórios.");
            }

            if (!isEdit && (pass == null || pass.length() < 4)) {
                throw new IllegalArgumentException("A senha inicial deve conter pelo menos 4 caracteres.");
            }

            return () -> {
                if (!isEdit) {
                    userApiClient.createUser(username, name, pass, role);
                } else {
                    userApiClient.updateUserName(username, name);
                    userApiClient.updateCompanyRole(username, role);
                }

                if (onSuccess != null) {
                    SwingUtilities.invokeLater(onSuccess);
                }
                return null;
            };
        });

        dialog.showDialog();
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }
}
