package mz.multicore.erp.gui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Ícone composto que renderiza um ícone base com um badge numérico ou indicador no canto superior direito.
 */
public class BadgedIcon implements Icon {

    private final Icon baseIcon;
    private int badgeCount;
    private Color badgeBg;
    private Color badgeFg;
    private final int extraRightPadding;

    public BadgedIcon(Icon baseIcon, int badgeCount, Color badgeBg, Color badgeFg) {
        this.baseIcon = baseIcon != null ? baseIcon : UIHelper.icon("fas-circle", 16);
        this.badgeCount = badgeCount;
        this.badgeBg = badgeBg != null ? badgeBg : UIHelper.REJECTED_RED;
        this.badgeFg = badgeFg != null ? badgeFg : Color.WHITE;
        this.extraRightPadding = 6;
    }

    public BadgedIcon(Icon baseIcon, int badgeCount, Color badgeBg) {
        this(baseIcon, badgeCount, badgeBg, Color.WHITE);
    }

    public BadgedIcon(Icon baseIcon, int badgeCount) {
        this(baseIcon, badgeCount, UIHelper.REJECTED_RED, Color.WHITE);
    }

    public int getBadgeCount() {
        return badgeCount;
    }

    public void setBadgeCount(int count) {
        this.badgeCount = count;
    }

    public void setBadgeBg(Color badgeBg) {
        this.badgeBg = badgeBg != null ? badgeBg : UIHelper.REJECTED_RED;
    }

    public void setBadgeFg(Color badgeFg) {
        this.badgeFg = badgeFg != null ? badgeFg : Color.WHITE;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            baseIcon.paintIcon(c, g2, x, y);

            if (badgeCount > 0) {
                String text = badgeCount > 99 ? "99+" : String.valueOf(badgeCount);
                Font badgeFont = new Font(UIHelper.FONT, Font.BOLD, 9);
                g2.setFont(badgeFont);
                FontMetrics fm = g2.getFontMetrics();

                int textW = fm.stringWidth(text);
                int badgeH = 14;
                int badgeW = Math.max(14, textW + 6);

                int bx = x + baseIcon.getIconWidth() - (badgeW / 2);
                int by = y - 2;

                // Fundo do badge com cantos arredondados
                g2.setColor(badgeBg);
                g2.fillRoundRect(bx, by, badgeW, badgeH, 8, 8);

                // Texto do badge
                g2.setColor(badgeFg);
                int textX = bx + (badgeW - textW) / 2;
                int textY = by + ((badgeH - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, textX, textY);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return baseIcon.getIconWidth() + (badgeCount > 0 ? extraRightPadding : 0);
    }

    @Override
    public int getIconHeight() {
        return baseIcon.getIconHeight();
    }
}
