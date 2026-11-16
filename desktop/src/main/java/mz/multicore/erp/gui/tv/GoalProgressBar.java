package mz.multicore.erp.gui.tv;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Polygon;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Barra de progresso de meta para ecrãs grandes: trilho discreto, preenchimento na cor do
 * {@link GoalStatus}, divisões em 25/50/75% e, opcionalmente, o marcador do ritmo esperado
 * (onde a barra devia estar hoje). Os números ficam fora da barra, em rótulos próprios.
 */
public class GoalProgressBar extends JPanel {

    private static final int MARKER_ROOM = 6;

    private BigDecimal current = BigDecimal.ZERO;
    private BigDecimal goal = BigDecimal.ZERO;
    private final int barHeight;
    private boolean lightMode;
    private double pacePercent = -1;

    public GoalProgressBar() {
        this(12);
    }

    public GoalProgressBar(int height) {
        this.barHeight = height;
        setOpaque(false);
        setPreferredSize(new Dimension(300, height + 2 * MARKER_ROOM));
    }

    public void setLightMode(boolean lightMode) {
        this.lightMode = lightMode;
        repaint();
    }

    public boolean isLightMode() {
        return lightMode;
    }

    public void setValues(BigDecimal current, BigDecimal goal) {
        this.current = current != null ? current : BigDecimal.ZERO;
        this.goal = goal != null && goal.signum() > 0 ? goal : BigDecimal.ZERO;
        repaint();
    }

    /** Percentagem esperada para hoje; valores negativos escondem o marcador. */
    public void setPaceMarker(double expectedPercent) {
        this.pacePercent = expectedPercent;
        repaint();
    }

    public double getPercentage() {
        if (goal.signum() <= 0) return 0.0;
        return current.multiply(BigDecimal.valueOf(100)).divide(goal, 1, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width = getWidth();
        if (width <= 10) return;
        Graphics2D g2 = ExecutiveTvComponents.antialiased(g);
        int h = Math.min(barHeight, getHeight() - 2 * MARKER_ROOM);
        int y = (getHeight() - h) / 2;
        int arc = h;

        g2.setColor(ExecutiveTvTheme.getTrackColor(lightMode));
        g2.fillRoundRect(0, y, width, h, arc, arc);

        double pct = getPercentage();
        GoalStatus status = GoalStatus.of(goal.signum() > 0, pct);
        int fillW = (int) Math.round(Math.min(pct, 100.0) / 100.0 * width);
        if (fillW > 0) {
            Color color = status.color(lightMode);
            g2.setPaint(new GradientPaint(0, y, ExecutiveTvTheme.tint(Color.WHITE, color, 0.12), fillW, y, color));
            g2.fillRoundRect(0, y, Math.max(fillW, arc), h, arc, arc);
        }

        Color notch = ExecutiveTvTheme.getCardBgColor(lightMode);
        g2.setStroke(new BasicStroke(2f));
        for (int mark : new int[]{25, 50, 75}) {
            int x = (int) Math.round(mark / 100.0 * width);
            if (x > fillW) {
                g2.setColor(notch);
                g2.drawLine(x, y + 2, x, y + h - 3);
            }
        }

        if (pacePercent >= 0 && goal.signum() > 0) {
            int x = (int) Math.round(Math.min(100.0, pacePercent) / 100.0 * (width - 2)) + 1;
            Color ink = ExecutiveTvTheme.getTextLightColor(lightMode);
            g2.setColor(ink);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(x, y - 3, x, y + h + 3);
            g2.fillPolygon(new Polygon(new int[]{x - 5, x + 5, x}, new int[]{y - MARKER_ROOM, y - MARKER_ROOM, y - 1}, 3));
        }
        g2.dispose();
    }
}
