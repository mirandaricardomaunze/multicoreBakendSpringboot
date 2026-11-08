package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

/**
 * Cartão centralizado de credenciais de login: uma única acção primária ("Entrar"),
 * deteção de Caps Lock, alternância de visibilidade da senha, "Lembrar utilizador"
 * e contas de demonstração ({@link LoginDemoAccountsPanel}, só com -Dmulticore.demo-login=true).
 */
class LoginCredentialsCard {

    private static final Color BRAND = UIHelper.ACCENT_BLUE;
    private static final Preferences PREFS = Preferences.userRoot().node("mz/multicore/erp/login");
    private static final String PREF_LAST_USER = "lastUser";

    private final ModernPanel card;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton togglePasswordBtn;
    private final JLabel capsLockWarning;
    private final JLabel errorLabel;
    private final JCheckBox rememberUser;
    private final ModernButton loginBtn;
    private final ModernButton cancelBtn;
    private final JProgressBar progressBar;

    private boolean passwordVisible = false;
    private final Runnable onLogin;

    LoginCredentialsCard(Runnable onLogin, Runnable onCancel) {
        this.onLogin = onLogin;

        card = new ModernPanel(16, Color.WHITE);
        card.putClientProperty("card.border", UIHelper.withAlpha(UIHelper.BORDER, 160));
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(24, 36, 18, 36));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0; c.gridy = 0;

        JLabel logo = new JLabel(new ImageIcon(UIHelper.getAppIcon(48)));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(logo, c);

        c.gridy++; c.insets = new Insets(10, 0, 0, 0);
        card.add(centered("Bem-vindo ao Multicore", Font.BOLD, 22, UIHelper.TEXT_LIGHT), c);

        c.gridy++; c.insets = new Insets(2, 0, 16, 0);
        card.add(centered("Entre com a sua conta para continuar", Font.PLAIN, 12, UIHelper.TEXT_MUTED), c);

        c.gridy++; c.insets = new Insets(0, 0, 6, 0);
        card.add(fieldLabel("Utilizador"), c);

        c.gridy++; c.insets = new Insets(0, 0, 10, 0);
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

        c.gridy++; c.insets = new Insets(0, 0, 4, 0);
        JPanel optionsRow = new JPanel(new BorderLayout()); optionsRow.setOpaque(false);
        rememberUser = new JCheckBox("Lembrar utilizador");
        rememberUser.setOpaque(false); rememberUser.setFocusPainted(false);
        rememberUser.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); rememberUser.setForeground(UIHelper.TEXT_MUTED);
        optionsRow.add(rememberUser, BorderLayout.WEST);
        JButton forgot = linkButton("Esqueceu a senha?", BRAND);
        forgot.addActionListener(e -> setMessage("Contacte o administrador do sistema para redefinir a senha.", UIHelper.TEXT_MUTED));
        optionsRow.add(forgot, BorderLayout.EAST);
        card.add(optionsRow, c);

        c.gridy++; c.insets = new Insets(0, 0, 6, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(errorLabel, c);

        c.gridy++; c.insets = new Insets(0, 0, 4, 0);
        loginBtn = new ModernButton("Entrar", BRAND, UIHelper.ACCENT_BLUE_HOVER);
        loginBtn.setIcon(UIHelper.icon("fas-sign-in-alt", 14, Color.WHITE));
        loginBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        loginBtn.setPreferredSize(new Dimension(0, 46));
        loginBtn.addActionListener(e -> onLogin.run());
        loginBtn.getAccessibleContext().setAccessibleName("Submeter autenticação");
        card.add(loginBtn, c);

        c.gridy++; c.insets = new Insets(0, 0, 2, 0);
        progressBar = UIHelper.createBusyBar();
        progressBar.setVisible(false);
        card.add(progressBar, c);

        c.gridy++; c.insets = new Insets(2, 0, 0, 0);
        cancelBtn = new ModernButton("Sair  (Esc)", Color.WHITE, UIHelper.ROW_ALT);
        cancelBtn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); cancelBtn.setForeground(UIHelper.TEXT_MUTED);
        cancelBtn.setPreferredSize(new Dimension(0, 30));
        cancelBtn.addActionListener(e -> onCancel.run());
        cancelBtn.getAccessibleContext().setAccessibleName("Cancelar autenticação");
        card.add(cancelBtn, c);

        if (Boolean.getBoolean("multicore.demo-login")) {
            c.gridy++; c.insets = new Insets(8, 0, 0, 0);
            card.add(LoginDemoAccountsPanel.build((u, pw) -> { fillCredentials(u, pw); onLogin.run(); }), c);
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

        card.setPreferredSize(new Dimension(440, card.getLayout().preferredLayoutSize(card).height));

        String lastUser = PREFS.get(PREF_LAST_USER, "");
        rememberUser.setSelected(!lastUser.isEmpty());
        usernameField.setText(lastUser);
    }

    ModernPanel getCard() { return card; }
    JTextField getUsernameField() { return usernameField; }
    JPasswordField getPasswordField() { return passwordField; }
    JButton getTogglePasswordButton() { return togglePasswordBtn; }
    ModernButton getCancelButton() { return cancelBtn; }
    ModernButton getLoginButton() { return loginBtn; }
    JLabel getCapsLockWarning() { return capsLockWarning; }
    boolean isPasswordVisible() { return passwordVisible; }

    /** Foco inicial: senha quando o utilizador já vem lembrado, senão o utilizador. */
    JComponent initialFocusTarget() { return usernameField.getText().isEmpty() ? usernameField : passwordField; }

    /** Chamado após autenticação bem sucedida: guarda ou esquece o último utilizador. */
    void rememberSuccessfulLogin() {
        try {
            if (rememberUser.isSelected()) PREFS.put(PREF_LAST_USER, usernameField.getText().trim());
            else PREFS.remove(PREF_LAST_USER);
        } catch (Exception ignored) { /* preferências indisponíveis: não bloqueia o login */ }
    }

    void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordField.setEchoChar(passwordVisible ? (char) 0 : '•');
        Color eyeColor = passwordVisible ? BRAND : UIHelper.TEXT_MUTED;
        togglePasswordBtn.setIcon(UIHelper.icon(passwordVisible ? "fas-eye-slash" : "fas-eye", 14, eyeColor));
        togglePasswordBtn.setToolTipText(passwordVisible ? "Ocultar senha" : "Mostrar senha");
        togglePasswordBtn.getAccessibleContext().setAccessibleName(passwordVisible ? "Ocultar senha" : "Mostrar senha");
    }

    private JButton buildPasswordToggleButton() {
        JButton btn = new JButton(UIHelper.icon("fas-eye", 14, UIHelper.TEXT_MUTED));
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
        setError(" ");
        loginBtn.requestFocusInWindow();
    }

    void setError(String message) { setMessage(message, UIHelper.REJECTED_RED); }

    private void setMessage(String message, Color color) {
        errorLabel.setForeground(color);
        errorLabel.setText(message != null && !message.isBlank() ? message : " ");
    }

    void setBusy(boolean value) {
        usernameField.setEnabled(!value);
        passwordField.setEnabled(!value);
        togglePasswordBtn.setEnabled(!value);
        cancelBtn.setEnabled(!value);
        loginBtn.setEnabled(!value);
        loginBtn.setText(value ? "A entrar..." : "Entrar");
        progressBar.setVisible(value);
    }

    private static JButton linkButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(0, 0, 0, 0));
        b.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); b.setForeground(color);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JLabel centered(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font(UIHelper.FONT, style, size)); l.setForeground(color);
        return l;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); l.setForeground(UIHelper.TEXT_LIGHT);
        return l;
    }

    /** Campo com ícone: o contorno de foco (cor da marca) é pintado no contentor, não no campo interno. */
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
        p.setBorder(UIHelper.fieldBorder(UIHelper.BORDER, 1));
        JLabel iconLabel = new JLabel(UIHelper.icon(iconCode, 15, UIHelper.TEXT_MUTED));
        iconLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        field.putClientProperty("noFocusBorder", Boolean.TRUE);
        field.setBorder(new EmptyBorder(8, 4, 8, trailing != null ? 4 : 10));
        field.setOpaque(false);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                p.setBorder(UIHelper.fieldBorder(BRAND, 2)); iconLabel.setIcon(UIHelper.icon(iconCode, 15, BRAND));
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                p.setBorder(UIHelper.fieldBorder(UIHelper.BORDER, 1)); iconLabel.setIcon(UIHelper.icon(iconCode, 15, UIHelper.TEXT_MUTED));
            }
        });
        if (field instanceof JTextField tf) { tf.setBackground(Color.WHITE); tf.setCaretColor(UIHelper.TEXT_LIGHT); }
        field.setForeground(UIHelper.TEXT_LIGHT);
        p.add(iconLabel, BorderLayout.WEST); p.add(field, BorderLayout.CENTER);
        if (trailing != null) p.add(trailing, BorderLayout.EAST);
        p.setPreferredSize(new Dimension(0, 44));
        return p;
    }
}
