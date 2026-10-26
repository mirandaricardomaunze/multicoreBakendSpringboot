package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AuthApiClient;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.AboutDialog;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Janela em ecrã inteiro de autenticação do Multicore ERP.
 * Apresenta imagem de fundo corporativa de acolhimento e cartão centralizado
 * com botões harmonizados de "Entrar" e "Cancelar".
 * Autentica contra a API do backend fora do EDT via SwingWorker.
 */
public class LoginDialog extends JFrame {

    private final AuthApiClient authApiClient;
    private final Consumer<DesktopSession> onAuthenticated;
    private final LoginCredentialsCard credentialsCard;

    private boolean busy = false;
    private DesktopSession authenticatedSession;

    public LoginDialog(AuthApiClient authApiClient) {
        this(authApiClient, null);
    }

    public LoginDialog(AuthApiClient authApiClient, Consumer<DesktopSession> onAuthenticated) {
        super("MULTICORE — Entrar");
        this.authApiClient = authApiClient;
        this.onAuthenticated = onAuthenticated;
        this.credentialsCard = new LoginCredentialsCard(this::tryLogin, this::onCancel);

        setType(Window.Type.NORMAL);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1280, 800);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(false);
        setIconImages(UIHelper.getAppIcons());
        getContentPane().setBackground(new Color(248, 250, 252));

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                toFront();
                requestFocus();
                if (credentialsCard.getUsernameField() != null) {
                    credentialsCard.getUsernameField().requestFocusInWindow();
                }
            }
        });

        LoginBackgroundPanel backgroundPanel = new LoginBackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());
        backgroundPanel.add(buildTopBar(), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(buildContent());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        backgroundPanel.add(scrollPane, BorderLayout.CENTER);

        backgroundPanel.add(buildFooter(), BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(backgroundPanel, BorderLayout.CENTER);

        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public DesktopSession getAuthenticatedSession() { return authenticatedSession; }
    JTextField getUsernameField() { return credentialsCard.getUsernameField(); }
    JPasswordField getPasswordField() { return credentialsCard.getPasswordField(); }
    JButton getTogglePasswordButton() { return credentialsCard.getTogglePasswordButton(); }
    public ModernButton getCancelButton() { return credentialsCard.getCancelButton(); }
    public ModernButton getLoginButton() { return credentialsCard.getLoginButton(); }
    JLabel getCapsLockWarning() { return credentialsCard.getCapsLockWarning(); }
    boolean isPasswordVisible() { return credentialsCard.isPasswordVisible(); }
    void togglePasswordVisibility() { credentialsCard.togglePasswordVisibility(); }
    public ModernPanel getCard() { return credentialsCard.getCard(); }
    void fillCredentials(String username, String password) { credentialsCard.fillCredentials(username, password); }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(16, 24, 8, 24));

        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandBox.setOpaque(false);
        brandBox.add(new JLabel(new ImageIcon(UIHelper.getAppIcon(28))));
        JLabel brandName = new JLabel("MULTICORE ERP");
        brandName.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        brandName.setForeground(new Color(15, 23, 42));
        brandBox.add(brandName);
        bar.add(brandBox, BorderLayout.WEST);

        JLabel escHint = new JLabel("Pressione ESC para cancelar", UIHelper.icon("fas-info-circle", 12, new Color(100, 116, 139)), SwingConstants.RIGHT);
        escHint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        escHint.setForeground(new Color(71, 85, 105));
        bar.add(escHint, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 20, 16, 20));

        JLabel info = new JLabel("Multicore ERP v1.0.0 • Sistema Integrado de Gestão Empresarial • Moçambique");
        info.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        info.setForeground(new Color(71, 85, 105));
        info.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        info.setToolTipText("Multicore ERP • Clique para ver detalhes e licença do sistema");
        info.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { AboutDialog.show(LoginDialog.this); }
        });
        footer.add(info);
        return footer;
    }

    private JPanel buildContent() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.CENTER;
        wrapper.add(credentialsCard.getCard(), g);
        SwingUtilities.invokeLater(() -> credentialsCard.getUsernameField().requestFocusInWindow());
        return wrapper;
    }

    public void onCancel() {
        dispose();
        if (getDefaultCloseOperation() == JFrame.EXIT_ON_CLOSE) {
            System.exit(0);
        }
    }

    private void tryLogin() {
        if (busy) return;
        String username = credentialsCard.getUsernameField().getText().trim();
        String password = new String(credentialsCard.getPasswordField().getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            credentialsCard.setError("Indique utilizador e senha.");
            return;
        }

        setBusy(true);
        credentialsCard.setError(" ");
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
                    credentialsCard.setError(cause.getMessage());
                    credentialsCard.getPasswordField().setText("");
                    credentialsCard.getPasswordField().requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void setBusy(boolean value) {
        this.busy = value;
        credentialsCard.setBusy(value);
    }
}
