package com.phcpro.gui.components;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal top navigation bar. Holds a brand block (brand + active company),
 * a wrapping row of icon-only module buttons, and a trailing area for the
 * company selector / user chip. Responsibility: layout + active-item tracking.
 * No business logic.
 */
public class TopNavBar extends JPanel {

    // ── Cores sensíveis ao tema. Lidas a partir dos slots de UIHelper (claro/escuro) e não fixas,
    //    para a barra de topo acompanhar a troca de tema como o resto da janela. ─────────────────
    private static Color barBg()       { return UIHelper.isLight() ? new Color(255, 255, 255) : new Color(24, 32, 47); }
    private static Color barBorder()   { return UIHelper.isLight() ? new Color(226, 232, 240) : new Color(45, 55, 72); }
    private static Color headerText()  { return UIHelper.isLight() ? new Color(31, 41, 55)    : new Color(243, 244, 246); }
    private static Color subText()     { return UIHelper.isLight() ? new Color(100, 116, 139) : new Color(203, 213, 225); }

    private final JLabel brandLabel;
    private final JLabel brandSubLabel;
    private final JPanel navPanel;
    private final JPanel trailingPanel;
    private final List<TopNavItem> navItems = new ArrayList<>();

    public TopNavBar(String brand, String subBrand) {
        setLayout(new BorderLayout(12, 0));
        setBackground(barBg());
        setBorder(new EmptyBorder(6, 14, 6, 14));

        // ---- Brand block (left)
        brandLabel = new JLabel(brand);
        brandLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        brandLabel.setForeground(headerText());

        brandSubLabel = new JLabel(subBrand);
        brandSubLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        brandSubLabel.setForeground(subText());
        brandSubLabel.setToolTipText("Empresa ativa");

        JPanel brandStack = new JPanel();
        brandStack.setOpaque(false);
        brandStack.setLayout(new BoxLayout(brandStack, BoxLayout.Y_AXIS));
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandSubLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandStack.add(brandLabel);
        brandStack.add(Box.createRigidArea(new Dimension(0, 1)));
        brandStack.add(brandSubLabel);

        JPanel brandWrapper = new JPanel(new BorderLayout());
        brandWrapper.setOpaque(false);
        brandWrapper.setBorder(new EmptyBorder(0, 0, 0, 8));
        brandWrapper.add(brandStack, BorderLayout.CENTER);
        add(brandWrapper, BorderLayout.WEST);

        // ---- Nav buttons (center, wrapping to a 2nd line when needed)
        navPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 2, 2));
        navPanel.setOpaque(false);
        add(navPanel, BorderLayout.CENTER);

        // ---- Trailing area (right): company selector + user chip
        trailingPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        trailingPanel.setOpaque(false);
        add(trailingPanel, BorderLayout.EAST);
    }

    /** Update the sub-brand line (active company name shown under the brand). */
    public void setSubBrand(String text) {
        brandSubLabel.setText(text == null || text.isBlank() ? "" : text);
    }

    /** Add an icon-only module button. Returns it so the caller can wire active-state. */
    public TopNavItem addItem(Icon icon, String tooltip, Color accent, Runnable onClick) {
        TopNavItem item = new TopNavItem(icon, tooltip, accent, () -> {
            setActive(tooltip);
            if (onClick != null) onClick.run();
        });
        navItems.add(item);
        navPanel.add(item);
        return item;
    }

    /** Add a component to the trailing (right) area, e.g. company combo or user chip. */
    public void addTrailing(JComponent component) {
        trailingPanel.add(component);
    }

    /** Highlight the nav item whose label matches the given key. */
    public void setActive(String activeKey) {
        for (TopNavItem item : navItems) {
            item.setActive(item.key() != null && item.key().equals(activeKey));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(barBg());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(barBorder());
            g2.fillRect(0, getHeight() - 1, getWidth(), 1);
        } finally {
            g2.dispose();
        }
    }

    /**
     * A FlowLayout that wraps to the next line and reports a correct preferred
     * height for the available width, so it can live in a BorderLayout.NORTH region.
     * Based on the well-known WrapLayout idiom.
     */
    private static final class WrapLayout extends FlowLayout {

        WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(java.awt.Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(java.awt.Container target) {
            Dimension d = layoutSize(target, false);
            d.width -= (getHgap() + 1);
            return d;
        }

        private Dimension layoutSize(java.awt.Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                int count = target.getComponentCount();
                for (int i = 0; i < count; i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        dim.width = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + vgap;
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    rowWidth += d.width + hgap;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                dim.width = Math.max(dim.width, rowWidth);
                dim.height += rowHeight;

                dim.width += insets.left + insets.right + hgap * 2;
                dim.height += insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }
    }
}
