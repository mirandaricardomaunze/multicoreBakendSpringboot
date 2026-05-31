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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Professional login dialog shown before the main application window.
 * Authenticates against AppUserService. On success the AppUser is exposed via
 * {@link #getAuthenticatedUser()}; on cancel/close the dialog returns null.
 */
public class LoginDialog extends JDialog {

    private final AppUserService appUserService;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private AppUser authenticatedUser;

    public LoginDialog(AppUserService appUserService) {
        super((Frame) null, "MULTICORE — Entrar", true);
        this.appUserService = appUserService;

        setSize(440, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setIconImage(UIHelper.iconImage("fas-cube", 64, UIHelper.ACCENT));
        getContentPane().setBackground(UIHelper.BG_DARK);

        setLayout(new BorderLayout());
        add(buildContent(), BorderLayout.CENTER);
    }

    public AppUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    private JPanel buildContent() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(24, 32, 24, 32));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.gridy = 0;

        // Brand
        JLabel logo = new JLabel(UIHelper.icon("fas-cube", 56, UIHelper.ACCENT));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        wrapper.add(logo, g);

        g.gridy++;
        g.insets = new Insets(10, 0, 0, 0);
        JLabel brand = new JLabel("MULTICORE");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 26));
        brand.setForeground(UIHelper.TEXT_LIGHT);
        brand.setHorizontalAlignment(SwingConstants.CENTER);
        wrapper.add(brand, g);

        g.gridy++;
        g.insets = new Insets(2, 0, 22, 0);
        JLabel subtitle = new JLabel("Aceda à sua conta para continuar");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        wrapper.add(subtitle, g);

        // Card with fields
        ModernPanel card = new ModernPanel(14, UIHelper.BG_CARD, UIHelper.BG_CARD);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(22, 22, 22, 22));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 6, 0);
        card.add(fieldLabel("Utilizador"), c);

        c.gridy++;
        c.insets = new Insets(0, 0, 14, 0);
        usernameField = new JTextField();
        UIHelper.styleTextField(usernameField);
        card.add(iconField("fas-user", usernameField), c);

        c.gridy++;
        c.insets = new Insets(0, 0, 6, 0);
        card.add(fieldLabel("Senha"), c);

        c.gridy++;
        c.insets = new Insets(0, 0, 6, 0);
        passwordField = new JPasswordField();
        UIHelper.styleTextField(passwordField);
        card.add(iconField("fas-lock", passwordField), c);

        c.gridy++;
        c.insets = new Insets(0, 0, 14, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        errorLabel.setForeground(UIHelper.REJECTED_RED);
        card.add(errorLabel, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        ModernButton loginBtn = new ModernButton("Entrar");
        loginBtn.setIcon(UIHelper.icon("fas-sign-in-alt", 14));
        loginBtn.setGradient(UIHelper.ACCENT, UIHelper.ACCENT_BLUE);
        loginBtn.setPreferredSize(new Dimension(0, 42));
        loginBtn.addActionListener(e -> tryLogin());
        card.add(loginBtn, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        ModernButton registerBtn = new ModernButton("Criar nova conta",
                new Color(55, 65, 81), new Color(75, 85, 99));
        registerBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        registerBtn.setPreferredSize(new Dimension(0, 38));
        registerBtn.addActionListener(e -> openRegisterDialog());
        card.add(registerBtn, c);

        g.gridy++;
        g.insets = new Insets(0, 0, 16, 0);
        wrapper.add(card, g);

        g.gridy++;
        g.insets = new Insets(0, 0, 0, 0);
        JLabel hint = new JLabel("Contas de demonstração: maria / joao / ana  •  senha: password");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hint.setForeground(UIHelper.TEXT_MUTED);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        wrapper.add(hint, g);

        // Submit on Enter
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) tryLogin();
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);

        SwingUtilities.invokeLater(() -> usernameField.requestFocusInWindow());
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
        p.setPreferredSize(new Dimension(0, 42));
        return p;
    }

    private void tryLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Indique utilizador e senha.");
            return;
        }
        try {
            authenticatedUser = appUserService.authenticate(username, password);
            errorLabel.setText(" ");
            dispose();
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    private void openRegisterDialog() {
        RegisterDialog reg = new RegisterDialog(this, appUserService);
        reg.setVisible(true);
        AppUser created = reg.getCreatedUser();
        if (created != null) {
            usernameField.setText(created.getUsername());
            passwordField.requestFocusInWindow();
            errorLabel.setForeground(UIHelper.APPROVED_GREEN);
            errorLabel.setText("Conta criada. Introduza a sua senha para entrar.");
        }
    }
}
