package mz.multicore.erp.gui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.math.BigDecimal;

/**
 * Alternative {@link SimplePieChart} view used when only one slice has a value. A solid 100% circle
 * adds little information, so this view emphasises the share, value, full bar, and zero categories.
 */
final class PieSingleShareView {

    private static final int LEFT = 16;
    private static final int TOP = 58;

    private PieSingleShareView() {
    }

    /** Returns the only positive slice index, or -1 when there are zero or multiple positive slices. */
    static int singlePositiveIndex(BigDecimal[] values) {
        int found = -1;
        for (int i = 0; values != null && i < values.length; i++) {
            if (values[i] != null && values[i].signum() > 0) {
                if (found >= 0) return -1;
                found = i;
            }
        }
        return found;
    }

    static void paint(Graphics2D g, String[] labels, BigDecimal[] values, Color[] colors, int index, int width, int height) {
        Color color = colors[index];
        int right = width - LEFT;

        g.setFont(new Font(UIHelper.FONT, Font.BOLD, 26));
        g.setColor(color);
        FontMetrics big = g.getFontMetrics();
        int baseline = TOP + big.getAscent() - 6;
        g.drawString("100%", LEFT, baseline);
        int textX = LEFT + big.stringWidth("100%") + 12;

        g.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        g.setColor(UIHelper.TEXT_LIGHT);
        g.drawString(label(labels, index), textX, baseline - 12);
        g.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        g.setColor(UIHelper.TEXT_MUTED);
        g.drawString(SimplePieChart.formatFull(values[index]), textX, baseline + 4);

        int barY = baseline + 18;
        g.setColor(color);
        g.fillRoundRect(LEFT, barY, right - LEFT, 12, 12, 12);

        int rowY = barY + 36;
        g.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        for (int i = 0; i < values.length && rowY < height - 6; i++) {
            if (i == index) continue;
            g.setColor(colors[i]);
            g.fillRoundRect(LEFT, rowY - 9, 10, 10, 3, 3);
            g.setColor(UIHelper.TEXT_MUTED);
            g.drawString(label(labels, i) + " · 0%", LEFT + 16, rowY);
            rowY += 22;
        }
    }

    private static String label(String[] labels, int i) {
        return i < labels.length && labels[i] != null ? labels[i] : "Item " + (i + 1);
    }
}
