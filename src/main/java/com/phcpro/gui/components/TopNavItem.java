package com.phcpro.gui.components;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Compact icon-only navigation button for the top navbar.
 * Responsibility: render one module icon, react to hover/active state, expose
 * the module label as tooltip. No business logic.
 */
public class TopNavItem extends JComponent {

    private static final int WIDTH = 48;
    private static final int HEIGHT = 42;
    private static final int CORNER = 10;
    // Overlay de hover: clareia sobre barra escura, escurece sobre barra clara.
    private static Color hoverOverlay() {
        return UIHelper.isLight() ? new Color(0, 0, 0, 18) : new Color(255, 255, 255, 18);
    }

    private final Icon icon;
    private final String key;
    private final Color accent;
    private final Runnable onClick;

    private boolean active = false;
    private boolean hover = false;

    public TopNavItem(Icon icon, String tooltip, Color accent, Runnable onClick) {
        this.icon = icon;
        this.key = tooltip;
        this.accent = accent;
        this.onClick = onClick;

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(tooltip);
        Dimension d = new Dimension(WIDTH, HEIGHT);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });
    }

    /** The label this item matches against for active-state tracking. */
    public String key() {
        return key;
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int rectW = w - 6;
            int rectH = h - 6;

            if (active) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 48));
                g2.fillRoundRect(3, 3, rectW, rectH, CORNER, CORNER);
                // Accent underline bar at the bottom.
                g2.setColor(accent);
                g2.fillRoundRect((w - 20) / 2, h - 5, 20, 3, 3, 3);
            } else if (hover) {
                g2.setColor(hoverOverlay());
                g2.fillRoundRect(3, 3, rectW, rectH, CORNER, CORNER);
            }

            if (icon != null) {
                int iconX = (w - icon.getIconWidth()) / 2;
                int iconY = (h - icon.getIconHeight()) / 2 - 1;
                icon.paintIcon(this, g2, iconX, iconY);
            }
        } finally {
            g2.dispose();
        }
    }
}
