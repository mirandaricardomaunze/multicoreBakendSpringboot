package com.phcpro.gui.components;

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

    public SimpleBarChart(String title) {
        this.title = title;
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 14, 16));
        setPreferredSize(new Dimension(260, 220));
    }

    public void setData(String[] labels, BigDecimal[] values, Color[] colors) {
        this.labels = labels;
        this.values = values;
        this.colors = colors;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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

        g.setColor(new Color(55, 65, 81));
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

        for (int i = 0; i < values.length; i++) {
            BigDecimal rawValue = values[i] == null ? BigDecimal.ZERO : values[i].abs();
            double ratio = rawValue.divide(max, 6, RoundingMode.HALF_UP).doubleValue();
            int barHeight = Math.max(4, (int) Math.round(chartHeight * ratio));
            int x = left + i * slot + Math.max(0, (slot - barWidth) / 2);
            int y = top + chartHeight - barHeight;

            Color barColor = i < colors.length && colors[i] != null ? colors[i] : UIHelper.ACCENT_BLUE;
            g.setColor(barColor);
            g.fillRoundRect(x, y, barWidth, barHeight, 10, 10);

            g.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            g.setColor(UIHelper.TEXT_LIGHT);
            String valueText = formatCompact(rawValue);
            int valueWidth = g.getFontMetrics().stringWidth(valueText);
            g.drawString(valueText, x + (barWidth - valueWidth) / 2, Math.max(42, y - 7));

            g.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
            g.setColor(UIHelper.TEXT_MUTED);
            String label = i < labels.length ? labels[i] : "";
            int labelWidth = g.getFontMetrics().stringWidth(label);
            g.drawString(label, x + (barWidth - labelWidth) / 2, top + chartHeight + 22);
        }

        g.dispose();
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
