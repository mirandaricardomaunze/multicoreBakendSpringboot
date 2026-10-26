package mz.multicore.erp.gui.components;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Cabeçalho de seção semântica para a barra lateral retrátil com divisória suave.
 */
public class SidebarSectionHeader extends JPanel {

    private final JLabel label;
    private final HairlineDivider divider;

    public SidebarSectionHeader(String title, boolean showDivider) {
        this(title, showDivider, null);
    }

    public SidebarSectionHeader(String title, boolean showDivider, javax.swing.Icon icon) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, showDivider ? 32 : 18));

        if (showDivider) {
            add(Box.createRigidArea(new Dimension(0, 8)));
            divider = new HairlineDivider();
            add(divider);
            add(Box.createRigidArea(new Dimension(0, 8)));
        } else {
            divider = null;
            add(Box.createRigidArea(new Dimension(0, 4)));
        }

        label = new JLabel(title.toUpperCase());
        if (icon != null) {
            label.setIcon(icon);
            label.setIconTextGap(5);
        }
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        label.setForeground(UIHelper.isLight() ? new Color(148, 163, 184) : new Color(156, 163, 175));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(0, 10, 3, 0));
        add(label);
    }

    public void setCollapsed(boolean collapsed) {
        label.setVisible(!collapsed);
        if (divider != null) { divider.setCollapsed(collapsed); divider.repaint(); }
        revalidate();
        repaint();
    }

    /** Linha divisória fina que se adapta ao modo recolhido e expandido. */
    private static final class HairlineDivider extends JComponent {
        private boolean collapsed = false;

        HairlineDivider() {
            setPreferredSize(new Dimension(20, 1));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setCollapsed(boolean collapsed) { this.collapsed = collapsed; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                int w = getWidth();
                g2.setColor(UIHelper.isLight() ? new Color(226, 232, 240, 180) : new Color(51, 65, 85, 160));
                if (collapsed) {
                    int lineW = 24;
                    int lx = (w - lineW) / 2;
                    g2.fillRect(lx, 0, lineW, 1);
                } else {
                    g2.fillRect(8, 0, Math.max(0, w - 16), 1);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
