package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.service.AppUserService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Registration dialog for creating a new AppUser from the login screen.
 * Returns the created user via {@link #getCreatedUser()} or null on cancel.
 */
public class RegisterDialog extends JDialog {

    private final AppUserService appUserService;

    private JTextField usernameField;
    private JTextField nameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JComboBox<String> roleCombo;
    private JLabel errorLabel;
    private AppUser createdUser;

    public RegisterDialog(Window parent, AppUserService appUserService) {
        super(parent, "MULTICORE — Criar Conta", ModalityType.APPLICATION_MODAL);
        this.appUserService = appUserService;

        setSize(440, 620);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setIconImage(UIHelper.iconImage("fas-user-plus", 64, UIHelper.APPROVED_GREEN));
        getContentPane().setBackground(UIHelper.BG_DARK);

        setLayout(new BorderLayout());
        add(buildContent(), BorderLayout.CENTER);
    }

    public AppUser getCreatedUser() {
        return createdUser;
    }

    private JPanel buildContent() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(24, 32, 24, 32));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        JLabel logo = new JLabel(UIHelper.icon("fas-user-plus", 48, UIHelper.APPROVED_GREEN));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        wrapper.add(logo, g);

        g.gridy++;
        g.insets = new Insets(10, 0, 0, 0);
        JLabel brand = new JLabel("Criar Nova Conta");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 22));
        brand.setForeground(UIHelper.TEXT_LIGHT);
        brand.setHorizontalAlignment(SwingConstants.CENTER);
        wrapper.add(brand, g);

        g.gridy++;
        g.insets = new Insets(2, 0, 22, 0);
        JLabel subtitle = new JLabel("Preencha os dados para registar o seu acesso");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        wrapper.add(subtitle, g);

        ModernPanel card = new ModernPanel(14, UIHelper.BG_CARD, UIHelper.BG_CARD);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        int row = 0;

        c.gridy = row++;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(fieldLabel("Nome Completo"), c);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 10, 0);
        nameField = new JTextField();
        UIHelper.styleTextField(nameField);
        card.add(iconField("fas-id-card", nameField), c);

        c.gridy = row++;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(fieldLabel("Utilizador"), c);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 10, 0);
        usernameField = new JTextField();
        UIHelper.styleTextField(usernameField);
        card.add(iconField("fas-user", usernameField), c);

        c.gridy = row++;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(fieldLabel("Senha"), c);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 10, 0);
        passwordField = new JPasswordField();
        UIHelper.styleTextField(passwordField);
        card.add(iconField("fas-lock", passwordField), c);

        c.gridy = row++;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(fieldLabel("Confirmar Senha"), c);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 10, 0);
        confirmField = new JPasswordField();
        UIHelper.styleTextField(confirmField);
        card.add(iconField("fas-lock", confirmField), c);

        c.gridy = row++;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(fieldLabel("Papel"), c);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 10, 0);
        roleCombo = new JComboBox<>(new String[]{"EMPLOYEE", "MANAGER", "ADMIN"});
        UIHelper.styleComboBox(roleCombo);
        card.add(roleCombo, c);

        c.gridy = row++;
        c.insets = new Insets(4, 0, 10, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        errorLabel.setForeground(UIHelper.REJECTED_RED);
        card.add(errorLabel, c);

        c.gridy = row++;
        c.insets = new Insets(0, 0, 8, 0);
        ModernButton createBtn = UIHelper.createSuccessButton("Criar Conta");
        createBtn.setIcon(UIHelper.icon("fas-check", 14));
        createBtn.setPreferredSize(new Dimension(0, 42));
        createBtn.addActionListener(e -> tryRegister());
        card.add(createBtn, c);

        c.gridy = row++;
        c.insets = new Insets(0, 0, 0, 0);
        ModernButton cancelBtn = UIHelper.createSecondaryButton("Voltar ao Login");
        cancelBtn.setIcon(UIHelper.icon("fas-arrow-left", 14));
        cancelBtn.setPreferredSize(new Dimension(0, 38));
        cancelBtn.addActionListener(e -> dispose());
        card.add(cancelBtn, c);

        g.gridy++;
        g.insets = new Insets(0, 0, 0, 0);
        wrapper.add(card, g);

        SwingUtilities.invokeLater(() -> nameField.requestFocusInWindow());
        return wrapper;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    private JPanel iconField(String iconCode, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(new Color(31, 41, 55));
        p.setBorder(new LineBorder(new Color(55, 65, 81), 1, true));
        JLabel iconLabel = new JLabel(UIHelper.icon(iconCode, 16, UIHelper.TEXT_MUTED));
        iconLabel.setBorder(new EmptyBorder(0, 12, 0, 0));
        field.setBorder(new EmptyBorder(10, 4, 10, 12));
        if (field instanceof JTextField tf) tf.setBackground(new Color(31, 41, 55));
        field.setForeground(UIHelper.TEXT_LIGHT);
        p.add(iconLabel, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(0, 40));
        return p;
    }

    private void tryRegister() {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String pwd = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());
        String role = (String) roleCombo.getSelectedItem();

        if (name.isEmpty() || username.isEmpty() || pwd.isEmpty()) {
            errorLabel.setText("Nome, utilizador e senha são obrigatórios.");
            return;
        }
        if (pwd.length() < 4) {
            errorLabel.setText("A senha deve ter pelo menos 4 caracteres.");
            return;
        }
        if (!pwd.equals(confirm)) {
            errorLabel.setText("As senhas não coincidem.");
            return;
        }
        try {
            createdUser = appUserService.createUser(username, name, pwd, role);
            dispose();
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
        }
    }
}
