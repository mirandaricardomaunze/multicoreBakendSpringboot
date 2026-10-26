package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Cartão centralizado de credenciais de login com acessibilidade,
 * deteção de Caps Lock e alternância de visibilidade de senha.
 */
class LoginCredentialsCard {

    private final ModernPanel card;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton togglePasswordBtn;
    private final JLabel capsLockWarning;
    private final JLabel errorLabel;
    private final ModernButton loginBtn;
    private final ModernButton cancelBtn;
    private final JProgressBar progressBar;

    private boolean passwordVisible = false;
    private final Runnable onLogin;
    private final Runnable onCancel;

    LoginCredentialsCard(Runnable onLogin, Runnable onCancel) {
        this.onLogin = onLogin;
        this.onCancel = onCancel;

        card = new ModernPanel(20, new Color(255, 255, 255, 248));
        card.putClientProperty("card.border", new Color(203, 213, 225, 220));
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(28, 32, 26, 32));
        card.setPreferredSize(new Dimension(460, 540));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0; c.gridy = 0;

        JLabel logo = new JLabel(new ImageIcon(UIHelper.getAppIcon(64)));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(logo, c);

        c.gridy++; c.insets = new Insets(10, 0, 0, 0);
        JLabel brand = new JLabel("MULTICORE");
        brand.setFont(new Font(UIHelper.FONT, Font.BOLD, 24));
        brand.setForeground(new Color(15, 23, 42));
        brand.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(brand, c);

        c.gridy++; c.insets = new Insets(2, 0, 18, 0);
        JLabel subtitle = new JLabel("Centro de Operações e Gestão Comercial");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(new Color(100, 116, 139));
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(subtitle, c);

        c.gridy++; c.insets = new Insets(0, 0, 6, 0);
        card.add(fieldLabel("Utilizador"), c);

        c.gridy++; c.insets = new Insets(0, 0, 12, 0);
        usernameField = new JTextField();
        UIHelper.styleTextField(usernameField);
        usernameField.getAccessibleContext().setAccessibleName("Nome de utilizador");
        card.add(iconField("fas-user", usernameField, null), c);

        c.gridy++; c.insets = new Insets(0, 0, 6, 0);
        JPanel passLabelRow = new JPanel(new BorderLayout()); passLabelRow.setOpaque(false);
        passLabelRow.add(fieldLabel("Senha"), BorderLayout.WEST);

        capsLockWarning = new JLabel("Caps Lock activo", UIHelper.icon("fas-exclamation-triangle", 11, UIHelper.PENDING_YELLOW), SwingConstants.RIGHT);
        capsLockWarning.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        capsLockWarning.setForeground(UIHelper.PENDING_YELLOW);
        capsLockWarning.setVisible(false);
        passLabelRow.add(capsLockWarning, BorderLayout.EAST);
        card.add(passLabelRow, c);

        c.gridy++; c.insets = new Insets(0, 0, 6, 0);
        passwordField = new JPasswordField();
        UIHelper.styleTextField(passwordField);
        passwordField.getAccessibleContext().setAccessibleName("Senha de acesso");

        togglePasswordBtn = buildPasswordToggleButton();
        card.add(iconField("fas-lock", passwordField, togglePasswordBtn), c);

        c.gridy++; c.insets = new Insets(0, 0, 10, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        errorLabel.setForeground(UIHelper.REJECTED_RED);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(errorLabel, c);

        c.gridy++; c.insets = new Insets(0, 0, 10, 0);
        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 12, 0)); buttonRow.setOpaque(false);

        cancelBtn = new ModernButton("Cancelar", UIHelper.REJECTED_RED, UIHelper.REJECTED_RED_HOVER);
        cancelBtn.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        cancelBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 13)); cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setPreferredSize(new Dimension(0, 44));
        cancelBtn.addActionListener(e -> onCancel.run());
        cancelBtn.getAccessibleContext().setAccessibleName("Cancelar autenticação");
        buttonRow.add(cancelBtn);

        loginBtn = new ModernButton("Entrar", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
        loginBtn.setIcon(UIHelper.icon("fas-sign-in-alt", 14, Color.WHITE));
        loginBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 13)); loginBtn.setForeground(Color.WHITE);
        loginBtn.setPreferredSize(new Dimension(0, 44));
        loginBtn.addActionListener(e -> onLogin.run());
        loginBtn.getAccessibleContext().setAccessibleName("Submeter autenticação");
        buttonRow.add(loginBtn);
        card.add(buttonRow, c);

        c.gridy++; c.insets = new Insets(0, 0, 2, 0);
        progressBar = UIHelper.createBusyBar();
        progressBar.setVisible(false);
        card.add(progressBar, c);

        if (Boolean.getBoolean("multicore.demo-login")) {
            c.gridy++; c.insets = new Insets(10, 0, 0, 0);
            card.add(buildDemoAccountsPanel(), c);
        }

        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                checkCapsLock(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) onLogin.run();
            }
            @Override public void keyReleased(KeyEvent e) { checkCapsLock(e); }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);
        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { checkCapsLock(null); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { capsLockWarning.setVisible(false); }
        });
    }

    ModernPanel getCard() { return card; }
    JTextField getUsernameField() { return usernameField; }
    JPasswordField getPasswordField() { return passwordField; }
    JButton getTogglePasswordButton() { return togglePasswordBtn; }
    ModernButton getCancelButton() { return cancelBtn; }
    ModernButton getLoginButton() { return loginBtn; }
    JLabel getCapsLockWarning() { return capsLockWarning; }
    boolean isPasswordVisible() { return passwordVisible; }

    void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordField.setEchoChar(passwordVisible ? (char) 0 : '•');
        Color eyeColor = passwordVisible ? UIHelper.ACCENT_BLUE : new Color(100, 116, 139);
        togglePasswordBtn.setIcon(UIHelper.icon(passwordVisible ? "fas-eye-slash" : "fas-eye", 14, eyeColor));
        togglePasswordBtn.setToolTipText(passwordVisible ? "Ocultar senha" : "Mostrar senha");
        togglePasswordBtn.getAccessibleContext().setAccessibleName(passwordVisible ? "Ocultar senha" : "Mostrar senha");
    }

    private JButton buildPasswordToggleButton() {
        JButton btn = new JButton(UIHelper.icon("fas-eye", 14, new Color(100, 116, 139)));
        btn.setPreferredSize(new Dimension(34, 34)); btn.setFocusable(false);
        btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Mostrar senha");
        btn.getAccessibleContext().setAccessibleName("Mostrar senha");
        btn.addActionListener(e -> togglePasswordVisibility());
        return btn;
    }

    void checkCapsLock(KeyEvent e) {
        boolean capsOn = false;
        try { capsOn = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK); } catch (Exception ignored) {}
        if (e != null && e.getKeyCode() == KeyEvent.VK_CAPS_LOCK) capsOn = !capsOn;
        capsLockWarning.setVisible(capsOn);
    }

    void fillCredentials(String username, String password) {
        usernameField.setText(username);
        passwordField.setText(password);
        errorLabel.setText(" ");
        loginBtn.requestFocusInWindow();
    }

    void setError(String message) {
        errorLabel.setText(message != null ? message : " ");
    }

    void setBusy(boolean value) {
        usernameField.setEnabled(!value);
        passwordField.setEnabled(!value);
        if (togglePasswordBtn != null) togglePasswordBtn.setEnabled(!value);
        if (cancelBtn != null) cancelBtn.setEnabled(!value);
        loginBtn.setEnabled(!value);
        loginBtn.setText(value ? "A entrar..." : "Entrar");
        progressBar.setVisible(value);
    }

    private JPanel buildDemoAccountsPanel() {
        JPanel panel = new JPanel(); panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Ambiente de demonstração — clique para entrar:");
        title.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        title.setForeground(new Color(100, 116, 139)); title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title); panel.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel chipsRow1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0)); chipsRow1.setOpaque(false);
        chipsRow1.add(createDemoChip("Maria (Gestão)", "maria", "password"));
        chipsRow1.add(createDemoChip("João (Caixa)", "joao", "password"));
        chipsRow1.add(createDemoChip("Ana (RH)", "ana", "password"));
        panel.add(chipsRow1);

        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        JPanel chipsRow2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0)); chipsRow2.setOpaque(false);
        chipsRow2.add(createDemoChip("Admin (Total)", "admin", "admin"));
        chipsRow2.add(createDemoChip("Superadmin (Plataforma)", "superadmin", "superadmin"));
        panel.add(chipsRow2);
        return panel;
    }

    private ModernButton createDemoChip(String label, String username, String password) {
        ModernButton btn = new ModernButton(label, new Color(241, 245, 249), new Color(226, 232, 240));
        btn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11)); btn.setForeground(new Color(30, 41, 59));
        btn.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(203, 213, 225), 1, true), new EmptyBorder(4, 8, 4, 8)));
        btn.setToolTipText("Entrar imediatamente como " + username);
        btn.addActionListener(e -> {
            fillCredentials(username, password);
            onLogin.run();
        });
        return btn;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 11)); l.setForeground(new Color(51, 65, 85));
        return l;
    }

    private JPanel iconField(String iconCode, JComponent field, JComponent trailing) {
        JPanel p = new JPanel(new BorderLayout(6, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UIHelper.CONTROL_ARC, UIHelper.CONTROL_ARC);
                g2.dispose();
            }
        };
        p.setOpaque(false); p.setBackground(Color.WHITE);
        p.setBorder(UIHelper.fieldBorder(new Color(203, 213, 225), 1));
        JLabel iconLabel = new JLabel(UIHelper.icon(iconCode, 15, new Color(100, 116, 139)));
        iconLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        field.setBorder(new EmptyBorder(8, 4, 8, trailing != null ? 4 : 10));
        field.setOpaque(false);
        if (field instanceof JTextField tf) {
            tf.setBackground(Color.WHITE);
            tf.setCaretColor(new Color(15, 23, 42));
        }
        field.setForeground(new Color(15, 23, 42));
        p.add(iconLabel, BorderLayout.WEST); p.add(field, BorderLayout.CENTER);
        if (trailing != null) p.add(trailing, BorderLayout.EAST);
        p.setPreferredSize(new Dimension(0, 42));
        return p;
    }
}
