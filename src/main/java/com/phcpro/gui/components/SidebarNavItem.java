package com.phcpro.gui.components;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Single navigation item in the sidebar.
 * Responsibility: render one icon + label row, react to hover / active state,
 * and adapt layout when the sidebar collapses.
 */
public class SidebarNavItem extends JComponent {

    private static final int ITEM_HEIGHT = 44;
    private static final int ICON_COLUMN = 56;
    private static final int CORNER = 10;
    private static final int LEFT_INSET = 10;
    private static final int RIGHT_INSET = 10;

    private static final Color TEXT_ACTIVE = Color.WHITE;
    private static final Color TEXT_INACTIVE = new Color(209, 213, 219);
    private static final Color HOVER_OVERLAY = new Color(255, 255, 255, 16);

    private final String iconGlyph;
    private final Icon icon;
    private final String label;
    private final Color accent;
    private final Runnable onClick;

    private boolean active = false;
    private boolean collapsed = false;
    private boolean hover = false;

    public SidebarNavItem(String iconGlyph, String label, Color accent, Runnable onClick) {
        this(iconGlyph, null, label, accent, onClick);
    }

    public SidebarNavItem(Icon icon, String label, Color accent, Runnable onClick) {
        this(null, icon, label, accent, onClick);
    }

    private SidebarNavItem(String iconGlyph, Icon icon, String label, Color accent, Runnable onClick) {
        this.iconGlyph = iconGlyph;
        this.icon = icon;
        this.label = label;
        this.accent = accent;
        this.onClick = onClick;

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(label);
        applySize();

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            repaint();
        }
    }

    public void setCollapsed(boolean collapsed) {
        if (this.collapsed != collapsed) {
            this.collapsed = collapsed;
            applySize();
            revalidate();
            repaint();
        }
    }

    private void applySize() {
        int width = collapsed ? ICON_COLUMN : 220;
        Dimension d = new Dimension(width, ITEM_HEIGHT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, ITEM_HEIGHT));
        setPreferredSize(d);
        setMinimumSize(new Dimension(ICON_COLUMN, ITEM_HEIGHT));
        setAlignmentX(LEFT_ALIGNMENT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            paintBackground(g2, w, h);
            paintIcon(g2, h);
            if (!collapsed) {
                paintLabel(g2, h);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintBackground(Graphics2D g2, int w, int h) {
        int padX = 8;
        int rectW = Math.max(0, w - padX * 2);
        int rectH = h - 8;
        int rectY = 4;

        if (active) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 48));
            g2.fillRoundRect(padX, rectY, rectW, rectH, CORNER, CORNER);
            // Accent left bar
            g2.setColor(accent);
            g2.fillRoundRect(padX, rectY + 6, 3, rectH - 12, 3, 3);
        } else if (hover) {
            g2.setColor(HOVER_OVERLAY);
            g2.fillRoundRect(padX, rectY, rectW, rectH, CORNER, CORNER);
        }
    }

    private void paintIcon(Graphics2D g2, int h) {
        if (icon != null) {
            int iconX = (ICON_COLUMN - icon.getIconWidth()) / 2 + LEFT_INSET;
            int iconY = (h - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, iconX, iconY);
            return;
        }
        g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 17));
        FontMetrics fm = g2.getFontMetrics();
        int iconWidth = fm.stringWidth(iconGlyph);
        int iconX = (ICON_COLUMN - iconWidth) / 2 + LEFT_INSET;
        int iconY = (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(active ? TEXT_ACTIVE : new Color(229, 231, 235));
        g2.drawString(iconGlyph, iconX, iconY);
    }

    private void paintLabel(Graphics2D g2, int h) {
        g2.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        FontMetrics fm = g2.getFontMetrics();
        int labelY = (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(active ? TEXT_ACTIVE : TEXT_INACTIVE);
        int labelX = ICON_COLUMN + LEFT_INSET;
        int available = getWidth() - labelX - RIGHT_INSET;
        String drawn = fitToWidth(label, fm, available);
        g2.drawString(drawn, labelX, labelY);
    }

    private String fitToWidth(String text, FontMetrics fm, int available) {
        if (fm.stringWidth(text) <= available) return text;
        String ellipsis = "…";
        int eW = fm.stringWidth(ellipsis);
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (int i = 0; i < text.length(); i++) {
            int cw = fm.charWidth(text.charAt(i));
            if (used + cw + eW > available) break;
            sb.append(text.charAt(i));
            used += cw;
        }
        return sb.append(ellipsis).toString();
    }
}
