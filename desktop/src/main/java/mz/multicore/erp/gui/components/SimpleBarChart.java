package mz.multicore.erp.gui.components;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Gráfico de barras leve, desenhado à mão (sem dependências externas), para os painéis de
 * visão geral (Dashboard, RH, …). Mostra um título, eixo de base e barras arredondadas com o
 * valor por cima (formato compacto k/M) e a legenda por baixo. Reutilizável: cada painel passa
 * os seus próprios rótulos/valores/cores via {@link #setData}.
 */
public class SimpleBarChart extends JPanel {

    private final String title;
    private String[] labels = new String[0];
    private BigDecimal[] values = new BigDecimal[0];
    private Color[] colors = new Color[0];
    private java.awt.Rectangle[] barBounds = new java.awt.Rectangle[0];

    public SimpleBarChart(String title) {
        this.title = title;
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 14, 16));
        setPreferredSize(new Dimension(260, 220));

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int index = getBarIndexAt(e.getPoint());
                if (index >= 0 && index < values.length && values[index] != null) {
                    String lbl = index < labels.length && labels[index] != null ? labels[index] : "Item " + (index + 1);
                    setToolTipText(lbl + ": " + formatFull(values[index]));
                } else {
                    setToolTipText(null);
                }
            }
        });
    }

    public void setData(String[] labels, BigDecimal[] values, Color[] colors) {
        this.labels = labels != null ? labels : new String[0];
        this.values = values != null ? values : new BigDecimal[0];
        this.colors = colors != null ? colors : new Color[0];
        this.barBounds = new java.awt.Rectangle[this.values.length];
        repaint();
    }

    public int getBarIndexAt(java.awt.Point point) {
        if (point == null || barBounds == null) return -1;
        for (int i = 0; i < barBounds.length; i++) {
            if (barBounds[i] != null && barBounds[i].contains(point)) {
                return i;
            }
        }
        return -1;
    }

    public java.awt.Rectangle[] getBarBounds() {
        return barBounds != null ? barBounds.clone() : new java.awt.Rectangle[0];
    }

    public static String formatFull(BigDecimal value) {
        if (value == null) return "0,00 MT";
        return String.format("%,.2f MT", value);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int left = 36;
        int right = 18;
        int top = 48;
        int bottom = 42;
        int chartWidth = Math.max(1, width - left - right);
        int chartHeight = Math.max(1, height - top - bottom);

        g.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        g.setColor(UIHelper.TEXT_LIGHT);
        g.drawString(title, 16, 26);

        // 3 Linhas de benchmark de referência (25%, 50%, 75%)
        java.awt.Stroke origStroke = g.getStroke();
        java.awt.Stroke dashed = new java.awt.BasicStroke(1.0f, java.awt.BasicStroke.CAP_BUTT,
                java.awt.BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f);
        g.setStroke(dashed);
        g.setColor(UIHelper.isLight() ? new Color(226, 232, 240, 180) : new Color(55, 65, 81, 140));
        for (int step = 1; step <= 3; step++) {
            int lineY = top + chartHeight - (int) Math.round(chartHeight * (step * 0.25));
            g.drawLine(left, lineY, left + chartWidth, lineY);
        }
        g.setStroke(origStroke);

        // Linha de base do eixo
        g.setColor(UIHelper.isLight() ? new Color(203, 213, 225) : new Color(55, 65, 81));
        g.drawLine(left, top + chartHeight, left + chartWidth, top + chartHeight);

        if (values.length == 0) {
            g.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            g.setColor(UIHelper.TEXT_MUTED);
            g.drawString("Sem dados", left, top + 30);
            g.dispose();
            return;
        }

        BigDecimal max = BigDecimal.ONE;
        for (BigDecimal value : values) {
            if (value != null && value.abs().compareTo(max) > 0) {
                max = value.abs();
            }
        }

        int count = Math.max(1, values.length);
        int slot = Math.max(34, chartWidth / count);
        int barWidth = Math.max(22, Math.min(54, slot - 18));
        barBounds = new java.awt.Rectangle[values.length];

        for (int i = 0; i < values.length; i++) {
            BigDecimal rawValue = values[i] == null ? BigDecimal.ZERO : values[i].abs();
            double ratio = rawValue.divide(max, 6, RoundingMode.HALF_UP).doubleValue();
            int barHeight = Math.max(4, (int) Math.round(chartHeight * ratio));
            int x = left + i * slot + Math.max(0, (slot - barWidth) / 2);
            int y = top + chartHeight - barHeight;

            barBounds[i] = new java.awt.Rectangle(x, y, barWidth, barHeight);

            Color barColor = i < colors.length && colors[i] != null ? colors[i] : UIHelper.ACCENT_BLUE;
            Color topColor = lighten(barColor, 0.25f);

            // Gradiente vertical premium: topo iluminado, base canónica
            java.awt.GradientPaint gp = new java.awt.GradientPaint(x, y, topColor, x, y + barHeight, barColor);
            g.setPaint(gp);
            g.fillRoundRect(x, y, barWidth, barHeight, 10, 10);

            g.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            g.setColor(UIHelper.TEXT_LIGHT);
            String valueText = formatCompact(rawValue);
            int valueWidth = g.getFontMetrics().stringWidth(valueText);
            g.drawString(valueText, x + (barWidth - valueWidth) / 2, Math.max(42, y - 7));

            g.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
            g.setColor(UIHelper.TEXT_MUTED);
            String label = i < labels.length && labels[i] != null ? labels[i] : "";
            int labelWidth = g.getFontMetrics().stringWidth(label);
            g.drawString(label, x + (barWidth - labelWidth) / 2, top + chartHeight + 22);
        }

        g.dispose();
    }

    private static Color lighten(Color color, float factor) {
        if (color == null) return UIHelper.ACCENT_BLUE;
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(r, g, b, color.getAlpha());
    }

    private String formatCompact(BigDecimal value) {
        double number = value.doubleValue();
        if (Math.abs(number) >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000);
        }
        if (Math.abs(number) >= 1_000) {
            return String.format("%.1fk", number / 1_000);
        }
        return String.format("%.0f", number);
    }
}
