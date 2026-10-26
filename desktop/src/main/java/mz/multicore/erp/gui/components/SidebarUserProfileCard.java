package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Card de perfil do operador ativo com avatar circular, indicador online e botão de logout.
 */
public class SidebarUserProfileCard extends JPanel {

    private String displayName = "";
    private String role = "";
    private final JLabel nameLabel;
    private final JLabel roleLabel;
    private final AvatarWidget avatar;
    private final JPanel textStack;
    private final JButton logoutBtn;

    public SidebarUserProfileCard() {
        setOpaque(false);
        setLayout(new BorderLayout(8, 0));

        avatar = new AvatarWidget(30);
        avatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showUserMenu(avatar);
            }
        });

        Color headColor = UIHelper.isLight() ? new Color(31, 41, 55) : new Color(243, 244, 246);
        Color subColor = UIHelper.isLight() ? new Color(100, 116, 139) : new Color(203, 213, 225);

        nameLabel = new JLabel("Multicore");
        nameLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        nameLabel.setForeground(headColor);

        roleLabel = new JLabel("Sistema");
        roleLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        roleLabel.setForeground(subColor);

        textStack = new JPanel();
        textStack.setOpaque(false);
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.add(nameLabel);
        textStack.add(Box.createRigidArea(new Dimension(0, 1)));
        textStack.add(roleLabel);

        logoutBtn = new JButton(UIHelper.icon("fas-sign-out-alt", 13, UIHelper.REJECTED_RED));
        logoutBtn.setToolTipText("Terminar Sessão (Logout)");
        logoutBtn.setFocusPainted(false);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorder(new EmptyBorder(4, 4, 4, 4));
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> UIHelper.requestLogout(SidebarUserProfileCard.this));

        add(avatar, BorderLayout.WEST);
        add(textStack, BorderLayout.CENTER);
        add(logoutBtn, BorderLayout.EAST);
    }

    public void updateProfile(String name, String roleTitle) {
        this.displayName = (name != null && !name.isBlank()) ? name : "Utilizador";
        this.role = (roleTitle != null && !roleTitle.isBlank()) ? roleTitle : "Operador";
        nameLabel.setText(displayName);
        roleLabel.setText(role);
        avatar.setInitials(getInitials(displayName));
        String tip = displayName + " (" + role + ") — Clique para opções";
        setToolTipText(tip);
        avatar.setToolTipText(tip);
        repaint();
    }

    public void setCollapsed(boolean collapsed) {
        textStack.setVisible(!collapsed);
        logoutBtn.setVisible(!collapsed);
        revalidate();
        repaint();
    }

    private void showUserMenu(Component invoker) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                new EmptyBorder(4, 6, 4, 6)));
        JMenuItem header = new JMenuItem(displayName + " (" + role + ")");
        header.setEnabled(false);
        popup.add(header);
        popup.addSeparator();
        JMenuItem logoutItem = new JMenuItem("Terminar Sessão", UIHelper.icon("fas-sign-out-alt", 12, UIHelper.REJECTED_RED));
        logoutItem.setForeground(UIHelper.REJECTED_RED);
        logoutItem.addActionListener(ev -> UIHelper.requestLogout(SidebarUserProfileCard.this));
        popup.add(logoutItem);
        popup.show(invoker, 0, invoker.getHeight());
    }

    private static String getInitials(String name) {
        if (name == null || name.isBlank()) return "MC";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private static final class AvatarWidget extends JComponent {
        private String initials = "MC";

        AvatarWidget(int size) {
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        void setInitials(String in) {
            this.initials = (in != null && !in.isBlank()) ? in : "MC";
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                Color c1 = UIHelper.ACCENT_BLUE;
                Color c2 = new Color(29, 78, 216);
                g2.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
                g2.fillOval(0, 0, w, h);

                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawOval(0, 0, w - 1, h - 1);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(initials);
                int tx = (w - tw) / 2;
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initials, tx, ty);

                int dotSize = 8;
                int dotX = w - dotSize;
                int dotY = h - dotSize;
                Color sideBg = UIHelper.isLight() ? new Color(255, 255, 255) : new Color(24, 32, 47);
                g2.setColor(sideBg);
                g2.fillOval(dotX - 1, dotY - 1, dotSize + 2, dotSize + 2);
                g2.setColor(new Color(16, 185, 129));
                g2.fillOval(dotX, dotY, dotSize, dotSize);
            } finally {
                g2.dispose();
            }
        }
    }
}
