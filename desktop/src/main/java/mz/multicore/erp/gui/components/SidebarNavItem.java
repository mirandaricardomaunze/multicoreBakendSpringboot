package mz.multicore.erp.gui.components;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.accessibility.AccessibleContext;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Item de navegação individual da barra lateral.
 * Suporta renderização de ícone + rótulo, foco por teclado, acessibilidade,
 * badge numérico de notificações, estados hover/ativo e adaptação fluida
 * a temas claro e escuro.
 */
public class SidebarNavItem extends JComponent {

    public static final int ITEM_HEIGHT = 42;
    public static final int ICON_COLUMN = 56;
    private static final int CORNER = 10;
    private static final int LEFT_INSET = 8;
    private static final int RIGHT_INSET = 10;

    private static Color textActive()   { return UIHelper.isLight() ? new Color(15, 23, 42) : Color.WHITE; }
    private static Color textInactive() { return UIHelper.isLight() ? new Color(71, 85, 105) : Color.WHITE; }
    private static Color hoverOverlay() { return UIHelper.isLight() ? new Color(0, 0, 0, 14) : new Color(255, 255, 255, 16); }

    private final String iconGlyph;
    private final Icon icon;
    private final String label;
    private final Color accent;
    private final Runnable onClick;

    private boolean active = false;
    private boolean collapsed = false;
    private boolean hover = false;
    private boolean focused = false;
    private int badgeCount = 0;

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
        this.accent = accent != null ? accent : UIHelper.ACCENT_BLUE;
        this.onClick = onClick;

        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(label);
        getAccessibleContext().setAccessibleName(label);
        applySize();

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    showContextMenu(e);
                } else if (onClick != null && e.getButton() == MouseEvent.BUTTON1) {
                    onClick.run();
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    showContextMenu(e);
                }
            }
        });

        addFocusListener(new FocusListener() {
            @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
            @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
        });

        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "activate");
        getActionMap().put("activate", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (onClick != null) onClick.run();
            }
        });
    }

    private void showContextMenu(MouseEvent e) {
        if (label == null || label.isBlank()) return;
        boolean isFav = SidebarFavoritesManager.getInstance().isFavorite(label);
        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem toggleFavItem = new javax.swing.JMenuItem(
                isFav ? "Remover dos Favoritos" : "Fixar nos Favoritos",
                UIHelper.icon(isFav ? "fas-times" : "fas-star", 14, isFav ? UIHelper.REJECTED_RED : UIHelper.ACCENT_ORANGE)
        );
        toggleFavItem.addActionListener(evt -> SidebarFavoritesManager.getInstance().toggleFavorite(label));
        popup.add(toggleFavItem);

        if (isShowing()) {
            popup.show(this, e.getX(), e.getY());
        } else {
            SidebarFavoritesManager.getInstance().toggleFavorite(label);
        }
    }

    public String getIconGlyph() { return iconGlyph; }
    public Icon getIcon() { return icon; }
    public Color getAccent() { return accent; }
    public Runnable getOnClick() { return onClick; }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public int getBadgeCount() {
        return badgeCount;
    }

    public void setBadgeCount(int count) {
        if (this.badgeCount != count) {
            this.badgeCount = count;
            repaint();
        }
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
        int width = collapsed ? ICON_COLUMN : 224;
        Dimension d = new Dimension(width, ITEM_HEIGHT);
        setMaximumSize(new Dimension(collapsed ? ICON_COLUMN : Integer.MAX_VALUE, ITEM_HEIGHT));
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
            paintBadge(g2, w, h);
        } finally {
            g2.dispose();
        }
    }

    private void paintBackground(Graphics2D g2, int w, int h) {
        int areaW = collapsed ? ICON_COLUMN - 4 : w;
        int padX = 6;
        int rectW = Math.max(0, areaW - padX * 2);
        int rectH = h - 6;
        int rectY = 3;

        if (active) {
            // Preenchimento pill translúcido e suave com a cor semântica do módulo
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), UIHelper.isLight() ? 28 : 42));
            g2.fillRoundRect(padX, rectY, rectW, rectH, CORNER, CORNER);

            if (!collapsed) {
                // Barra vertical de destaque na margem esquerda
                g2.setColor(accent);
                g2.fillRoundRect(padX + 2, rectY + 6, 3, rectH - 12, 3, 3);
            } else {
                // No modo colapsado, elegante anel suave ao redor do pill
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140));
                g2.drawRoundRect(padX, rectY, rectW - 1, rectH - 1, CORNER, CORNER);
            }
        } else if (hover) {
            g2.setColor(hoverOverlay());
            g2.fillRoundRect(padX, rectY, rectW, rectH, CORNER, CORNER);
        }

        if (focused && !active) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
            g2.drawRoundRect(padX, rectY, rectW - 1, rectH - 1, CORNER, CORNER);
        }
    }

    private void paintIcon(Graphics2D g2, int h) {
        int iconW = (icon != null) ? icon.getIconWidth() : 16;
        int iconH = (icon != null) ? icon.getIconHeight() : 16;
        int colW = collapsed ? (ICON_COLUMN - 4) : ICON_COLUMN;
        int iconX = (colW - iconW) / 2;
        int iconY = (h - iconH) / 2;

        if (icon != null) {
            icon.paintIcon(this, g2, iconX, iconY);
            return;
        }
        g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        FontMetrics fm = g2.getFontMetrics();
        int glyphW = fm.stringWidth(iconGlyph);
        int glyphX = (colW - glyphW) / 2;
        int glyphY = (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(active ? textActive() : textInactive());
        g2.drawString(iconGlyph, glyphX, glyphY);
    }

    private void paintLabel(Graphics2D g2, int h) {
        g2.setFont(new Font(UIHelper.FONT, active ? Font.BOLD : Font.PLAIN, 13));
        FontMetrics fm = g2.getFontMetrics();
        int labelY = (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(active ? textActive() : textInactive());
        int labelX = ICON_COLUMN + 6;
        int badgeReserve = (badgeCount > 0) ? 36 : 0;
        int available = getWidth() - labelX - RIGHT_INSET - badgeReserve;
        String drawn = fitToWidth(label, fm, Math.max(20, available));
        g2.drawString(drawn, labelX, labelY);
    }

    private void paintBadge(Graphics2D g2, int w, int h) {
        if (badgeCount <= 0) return;
        String badgeText = badgeCount > 99 ? "99+" : String.valueOf(badgeCount);
        g2.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(badgeText);
        int badgeH = 16;
        int badgeW = Math.max(16, textW + 8);

        int bx;
        int by;
        if (collapsed) {
            bx = w - badgeW - 6;
            by = 4;
        } else {
            bx = w - badgeW - RIGHT_INSET;
            by = (h - badgeH) / 2;
        }

        g2.setColor(UIHelper.REJECTED_RED);
        g2.fillRoundRect(bx, by, badgeW, badgeH, 10, 10);
        g2.setColor(Color.WHITE);
        int tx = bx + (badgeW - textW) / 2;
        int ty = by + (badgeH + fm.getAscent() - fm.getDescent()) / 2 - 1;
        g2.drawString(badgeText, tx, ty);
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

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleSidebarNavItem();
        }
        return accessibleContext;
    }

    protected class AccessibleSidebarNavItem extends AccessibleJComponent {
        private static final long serialVersionUID = 1L;
    }
}
