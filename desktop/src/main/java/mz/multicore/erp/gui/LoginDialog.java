package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.desktop.client.AuthApiClient;
import mz.multicore.erp.desktop.session.DesktopSession;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Janela em ecrã inteiro de autenticação do Multicore ERP.
 * Apresenta imagem de fundo corporativa de acolhimento e cartão centralizado
 * com botões harmonizados de "Entrar" e "Cancelar".
 * Autentica contra a API do backend fora do EDT via SwingWorker.
 */
public class LoginDialog extends JFrame {

    private static final BufferedImage BG_IMAGE = loadBackgroundImage();

    private final AuthApiClient authApiClient;
    private final Consumer<DesktopSession> onAuthenticated;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton togglePasswordBtn;
    private JLabel capsLockWarning;
    private JLabel errorLabel;
    private ModernButton loginBtn;
    private ModernButton cancelBtn;
    private JProgressBar progressBar;
    private boolean busy = false;
    private boolean passwordVisible = false;
    private DesktopSession authenticatedSession;

    public LoginDialog(AuthApiClient authApiClient) {
        this(authApiClient, null);
    }

    public LoginDialog(AuthApiClient authApiClient, Consumer<DesktopSession> onAuthenticated) {
        super("MULTICORE — Entrar");
        this.authApiClient = authApiClient;
        this.onAuthenticated = onAuthenticated;

        setType(Window.Type.NORMAL);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1280, 800);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(false);
        setIconImages(UIHelper.getAppIcons());
        getContentPane().setBackground(UIHelper.BG_DARK);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                toFront();
                requestFocus();
                if (usernameField != null) {
                    usernameField.requestFocusInWindow();
                }
            }
        });

        // Painel de fundo com a imagem e camada de sobreposição
        BackgroundPanel backgroundPanel = new BackgroundPanel(BG_IMAGE);
        backgroundPanel.setLayout(new BorderLayout());

        // Barra superior subtil
        backgroundPanel.add(buildTopBar(), BorderLayout.NORTH);

        // Centro com scroll e cartão centralizado
        JScrollPane scrollPane = new JScrollPane(buildContent());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        backgroundPanel.add(scrollPane, BorderLayout.CENTER);

        // Rodapé com identificação
        backgroundPanel.add(buildFooter(), BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(backgroundPanel, BorderLayout.CENTER);

        // Tecla ESC para cancelar / sair
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static BufferedImage loadBackgroundImage() {
        try (InputStream is = LoginDialog.class.getResourceAsStream("/images/login-bg.jpg")) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch (Exception ex) {
            System.err.println("[MULTICORE] Imagem de fundo não carregada: " + ex.getMessage());
        }
        return null;
    }

    public DesktopSession getAuthenticatedSession() {
        return authenticatedSession;
    }

    JTextField getUsernameField() {
        return usernameField;
    }

    JPasswordField getPasswordField() {
        return passwordField;
    }

    JButton getTogglePasswordButton() {
        return togglePasswordBtn;
    }

    public ModernButton getCancelButton() {
        return cancelBtn;
    }

    public ModernButton getLoginButton() {
        return loginBtn;
    }

    JLabel getCapsLockWarning() {
        return capsLockWarning;
    }

    boolean isPasswordVisible() {
        return passwordVisible;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(16, 24, 8, 24));

        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandBox.setOpaque(false);
        brandBox.add(new JLabel(new ImageIcon(UIHelper.getAppIcon(28))));
        JLabel brandName = new JLabel("MULTICORE ERP");
        brandName.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        brandName.setForeground(new Color(241, 245, 249, 230));
        brandBox.add(brandName);
        bar.add(brandBox, BorderLayout.WEST);

        JLabel escHint = new JLabel("Pressione ESC para cancelar", UIHelper.icon("fas-info-circle", 12, new Color(203, 213, 225, 200)), SwingConstants.RIGHT);
        escHint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        escHint.setForeground(new Color(203, 213, 225, 200));
        bar.add(escHint, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 20, 16, 20));

        JLabel info = new JLabel("Multicore ERP • Sistema Integrado de Gestão Empresarial • Moçambique");
        info.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        info.setForeground(new Color(226, 232, 240, 170));
        footer.add(info);

        return footer;
    }

    private JPanel buildContent() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.anchor = GridBagConstraints.CENTER;

        // Cartão de credenciais com cantos arredondados e transparência moderna
        ModernPanel card = new ModernPanel(20, new Color(18, 26, 44, 235));
        card.putClientProperty("card.border", new Color(255, 255, 255, 40));
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(28, 32, 26, 32));
        card.setPreferredSize(new Dimension(460, 540));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridy = 0;

        // Logótipo e Marca
        JLabel logo = new JLabel(new ImageIcon(UIHelper.getAppIcon(64)));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(logo, c);

        c.gridy++;
        c.insets = new Insets(10, 0, 0, 0);
        JLabel brand = new JLabel("MULTICORE");
        brand.setFont(new Font(UIHelper.FONT, Font.BOLD, 24));
        brand.setForeground(UIHelper.TEXT_LIGHT);
        brand.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(brand, c);

        c.gridy++;
        c.insets = new Insets(2, 0, 18, 0);
        JLabel subtitle = new JLabel("Centro de Operações e Gestão Comercial");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(subtitle, c);

        // Campo Utilizador
        c.gridy++;
        c.insets = new Insets(0, 0, 6, 0);
        card.add(fieldLabel("Utilizador"), c);

        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        usernameField = new JTextField();
        UIHelper.styleTextField(usernameField);
        usernameField.getAccessibleContext().setAccessibleName("Nome de utilizador");
        card.add(iconField("fas-user", usernameField, null), c);

        // Campo Senha
        c.gridy++;
        c.insets = new Insets(0, 0, 6, 0);
        JPanel passLabelRow = new JPanel(new BorderLayout());
        passLabelRow.setOpaque(false);
        passLabelRow.add(fieldLabel("Senha"), BorderLayout.WEST);

        capsLockWarning = new JLabel("Caps Lock activo", UIHelper.icon("fas-exclamation-triangle", 11, UIHelper.PENDING_YELLOW), SwingConstants.RIGHT);
        capsLockWarning.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        capsLockWarning.setForeground(UIHelper.PENDING_YELLOW);
        capsLockWarning.setVisible(false);
        passLabelRow.add(capsLockWarning, BorderLayout.EAST);
        card.add(passLabelRow, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 6, 0);
        passwordField = new JPasswordField();
        UIHelper.styleTextField(passwordField);
        passwordField.getAccessibleContext().setAccessibleName("Senha de acesso");

        togglePasswordBtn = buildPasswordToggleButton();
        card.add(iconField("fas-lock", passwordField, togglePasswordBtn), c);

        c.gridy++;
        c.insets = new Insets(0, 0, 10, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        errorLabel.setForeground(UIHelper.REJECTED_RED);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(errorLabel, c);

        // Linha com os dois botões: Cancelar e Entrar com cores harmonizadas
        c.gridy++;
        c.insets = new Insets(0, 0, 10, 0);
        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonRow.setOpaque(false);

        // Botão Cancelar: Tom vermelho de destaque com contraste claro e contorno suave
        cancelBtn = new ModernButton("Cancelar", UIHelper.REJECTED_RED, UIHelper.REJECTED_RED_HOVER);
        cancelBtn.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        cancelBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setPreferredSize(new Dimension(0, 44));
        cancelBtn.addActionListener(e -> onCancel());
        cancelBtn.getAccessibleContext().setAccessibleName("Cancelar autenticação");
        buttonRow.add(cancelBtn);

        // Botão Entrar: Tom Azul Acção de alto destaque
        loginBtn = new ModernButton("Entrar", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
        loginBtn.setIcon(UIHelper.icon("fas-sign-in-alt", 14, Color.WHITE));
        loginBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setPreferredSize(new Dimension(0, 44));
        loginBtn.addActionListener(e -> tryLogin());
        loginBtn.getAccessibleContext().setAccessibleName("Submeter autenticação");
        buttonRow.add(loginBtn);

        card.add(buttonRow, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 2, 0);
        progressBar = UIHelper.createBusyBar();
        progressBar.setVisible(false);
        card.add(progressBar, c);

        // Contas de demonstração na base do formulário quando activo
        if (Boolean.getBoolean("multicore.demo-login")) {
            c.gridy++;
            c.insets = new Insets(10, 0, 0, 0);
            card.add(buildDemoAccountsPanel(), c);
        }

        wrapper.add(card, g);

        // Listeners de teclado
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                checkCapsLock(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) tryLogin();
            }
            @Override public void keyReleased(KeyEvent e) {
                checkCapsLock(e);
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);
        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { checkCapsLock(null); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { capsLockWarning.setVisible(false); }
        });

        SwingUtilities.invokeLater(() -> usernameField.requestFocusInWindow());
        return wrapper;
    }

    public void onCancel() {
        dispose();
        if (getDefaultCloseOperation() == JFrame.EXIT_ON_CLOSE) {
            System.exit(0);
        }
    }

    private JButton buildPasswordToggleButton() {
        JButton btn = new JButton(UIHelper.icon("fas-eye", 14, Color.WHITE));
        btn.setPreferredSize(new Dimension(34, 34));
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Mostrar senha");
        btn.getAccessibleContext().setAccessibleName("Mostrar senha");
        btn.addActionListener(e -> togglePasswordVisibility());
        return btn;
    }

    void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordField.setEchoChar(passwordVisible ? (char) 0 : '•');
        togglePasswordBtn.setIcon(UIHelper.icon(passwordVisible ? "fas-eye-slash" : "fas-eye", 14, Color.WHITE));
        togglePasswordBtn.setToolTipText(passwordVisible ? "Ocultar senha" : "Mostrar senha");
        togglePasswordBtn.getAccessibleContext().setAccessibleName(passwordVisible ? "Ocultar senha" : "Mostrar senha");
    }

    private void checkCapsLock(KeyEvent e) {
        boolean capsOn = false;
        try {
            capsOn = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch (Exception ignored) {}
        if (e != null && e.getKeyCode() == KeyEvent.VK_CAPS_LOCK) {
            capsOn = !capsOn;
        }
        capsLockWarning.setVisible(capsOn);
    }

    private JPanel buildDemoAccountsPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Ambiente de demonstração — clique para entrar:");
        title.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        title.setForeground(UIHelper.TEXT_MUTED);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel chipsRow1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        chipsRow1.setOpaque(false);
        chipsRow1.add(createDemoChip("Maria (Gestão)", "maria", "password"));
        chipsRow1.add(createDemoChip("João (Caixa)", "joao", "password"));
        chipsRow1.add(createDemoChip("Ana (RH)", "ana", "password"));
        panel.add(chipsRow1);

        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        JPanel chipsRow2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        chipsRow2.setOpaque(false);
        chipsRow2.add(createDemoChip("Admin (Total)", "admin", "admin"));
        chipsRow2.add(createDemoChip("Superadmin (Plataforma)", "superadmin", "superadmin"));
        panel.add(chipsRow2);

        return panel;
    }

    private ModernButton createDemoChip(String label, String username, String password) {
        ModernButton btn = new ModernButton(label, UIHelper.FIELD_BG, UIHelper.SELECTION_BG);
        btn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        btn.setForeground(UIHelper.TEXT_LIGHT);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIHelper.BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        btn.setToolTipText("Entrar imediatamente como " + username);
        btn.addActionListener(e -> {
            fillCredentials(username, password);
            tryLogin();
        });
        return btn;
    }

    void fillCredentials(String username, String password) {
        usernameField.setText(username);
        passwordField.setText(password);
        errorLabel.setText(" ");
        loginBtn.requestFocusInWindow();
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        l.setForeground(UIHelper.TEXT_MUTED);
        return l;
    }

    private JPanel iconField(String iconCode, JComponent field, JComponent trailing) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setBackground(UIHelper.FIELD_BG);
        p.setBorder(new LineBorder(UIHelper.BORDER, 1, true));
        JLabel iconLabel = new JLabel(UIHelper.icon(iconCode, 15, UIHelper.TEXT_MUTED));
        iconLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        field.setBorder(new EmptyBorder(8, 4, 8, trailing != null ? 4 : 10));
        if (field instanceof JTextField tf) {
            tf.setBackground(UIHelper.FIELD_BG);
            tf.setCaretColor(UIHelper.TEXT_LIGHT);
        }
        field.setForeground(UIHelper.TEXT_LIGHT);
        p.add(iconLabel, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        if (trailing != null) {
            p.add(trailing, BorderLayout.EAST);
        }
        p.setPreferredSize(new Dimension(0, 42));
        return p;
    }

    private void tryLogin() {
        if (busy) return;
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Indique utilizador e senha.");
            return;
        }

        setBusy(true);
        errorLabel.setText(" ");
        new SwingWorker<DesktopSession, Void>() {
            @Override
            protected DesktopSession doInBackground() {
                return authApiClient.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    authenticatedSession = get();
                    if (onAuthenticated != null) {
                        onAuthenticated.accept(authenticatedSession);
                    }
                    dispose();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    setBusy(false);
                    errorLabel.setText(cause.getMessage());
                    passwordField.setText("");
                    passwordField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void setBusy(boolean value) {
        this.busy = value;
        usernameField.setEnabled(!value);
        passwordField.setEnabled(!value);
        if (togglePasswordBtn != null) togglePasswordBtn.setEnabled(!value);
        if (cancelBtn != null) cancelBtn.setEnabled(!value);
        loginBtn.setEnabled(!value);
        loginBtn.setText(value ? "A entrar..." : "Entrar");
        progressBar.setVisible(value);
    }

    /**
     * Painel de fundo que renderiza a imagem com preservação de proporção ('cover')
     * e sobreposição elegante de profundidade e contraste.
     */
    private static class BackgroundPanel extends JPanel {
        private final BufferedImage image;

        BackgroundPanel(BufferedImage image) {
            super();
            this.image = image;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            if (image != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                double imgW = image.getWidth();
                double imgH = image.getHeight();
                double scale = Math.max((double) w / imgW, (double) h / imgH);
                int drawW = (int) Math.round(imgW * scale);
                int drawH = (int) Math.round(imgH * scale);
                int x = (w - drawW) / 2;
                int y = (h - drawH) / 2;

                g2.drawImage(image, x, y, drawW, drawH, null);

                // Sobreposição de escurecimento para contraste do cartão
                g2.setColor(new Color(15, 23, 42, 125));
                g2.fillRect(0, 0, w, h);

                // Gradiente vertical para dar profundidade estética e conforto visual
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(15, 23, 42, 90),
                        0, h, new Color(10, 15, 30, 210)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                g2.dispose();
            } else {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, UIHelper.BG_DARK, 0, h, new Color(10, 15, 26));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        }
    }
}
