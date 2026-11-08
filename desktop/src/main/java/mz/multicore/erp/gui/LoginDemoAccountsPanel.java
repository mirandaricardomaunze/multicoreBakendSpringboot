package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * Grelha 2x2 de contas de demonstração do ecrã de login (avatar com inicial, nome e papel).
 * Só é montada quando o arranque usa -Dmulticore.demo-login=true; nunca em produção.
 */
final class LoginDemoAccountsPanel {

    private LoginDemoAccountsPanel() {}

    static JPanel build(BiConsumer<String, String> onPick) {
        JPanel panel = new JPanel(new BorderLayout(0, 6)); panel.setOpaque(false);
        JLabel title = new JLabel("CONTAS DE DEMONSTRAÇÃO", SwingConstants.CENTER);
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 10)); title.setForeground(UIHelper.TEXT_MUTED);
        title.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.BORDER), new EmptyBorder(10, 0, 0, 0)));
        panel.add(title, BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(2, 2, 8, 8)); grid.setOpaque(false);
        grid.add(chip("Maria", "Gestão", "maria", "password", UIHelper.ACCENT_BLUE, onPick));
        grid.add(chip("João", "Caixa", "joao", "password", UIHelper.APPROVED_GREEN, onPick));
        grid.add(chip("Ana", "Recursos Humanos", "ana", "password", UIHelper.ACCENT_PINK, onPick));
        grid.add(chip("Admin", "Acesso total", "admin", "admin", UIHelper.PENDING_YELLOW, onPick));
        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private static JButton chip(String name, String role, String username, String password, Color tone,
                                BiConsumer<String, String> onPick) {
        JButton btn = new JButton("<html><b>" + name + "</b><br><font color='#64748b'>" + role + "</font></html>") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                g2.setColor(hover ? UIHelper.withAlpha(tone, 22) : UIHelper.ROW_ALT);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(hover ? tone : UIHelper.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setIcon(avatarIcon(name.substring(0, 1), tone));
        btn.setIconTextGap(10); btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false); btn.setRolloverEnabled(true);
        btn.setBorder(new EmptyBorder(5, 10, 5, 8));
        btn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11)); btn.setForeground(UIHelper.TEXT_LIGHT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Entrar imediatamente como " + username);
        btn.getAccessibleContext().setAccessibleName("Entrar como " + name + ", " + role);
        btn.addActionListener(e -> onPick.accept(username, password));
        return btn;
    }

    private static Icon avatarIcon(String initial, Color tone) {
        return new Icon() {
            @Override public int getIconWidth() { return 30; }
            @Override public int getIconHeight() { return 30; }
            @Override public void paintIcon(Component comp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(tone); g2.fillOval(x, y, 30, 30);
                g2.setColor(Color.WHITE); g2.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initial, x + (30 - fm.stringWidth(initial)) / 2, y + (30 - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
    }
}
