package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Barra de progresso executiva de alta definição para metas de venda e negócio.
 * Desenhada em Java 2D vetorial anti-aliased com gradientes semânticos, marcadores
 * de meta intermediária e indicação clara de percentagem para ecrãs de sala de reuniões.
 * Suporta tanto o Modo Escuro (Dark Slate) quanto o Modo Claro (Executive Light).
 */
public class GoalProgressBar extends JPanel {

    private BigDecimal current = BigDecimal.ZERO;
    private BigDecimal goal = BigDecimal.ZERO;
    private String unitLabel = "MT";
    private boolean showDetailedLabels = true;
    private int barHeight = 28;
    private boolean lightMode = false;

    // Cores de fundo e trilha no modo escuro
    private static final Color TRACK_BG_DARK = new Color(30, 41, 59);
    private static final Color TRACK_BORDER_DARK = new Color(51, 65, 85);
    private static final Color MARKER_DARK = new Color(255, 255, 255, 45);

    // Cores de fundo e trilha no modo claro
    private static final Color TRACK_BG_LIGHT = new Color(241, 245, 249);
    private static final Color TRACK_BORDER_LIGHT = new Color(203, 213, 225);
    private static final Color MARKER_LIGHT = new Color(100, 116, 139, 60);

    public GoalProgressBar() {
        setOpaque(false);
        setPreferredSize(new Dimension(300, 48));
    }

    public GoalProgressBar(int height) {
        this.barHeight = height;
        setOpaque(false);
        setPreferredSize(new Dimension(300, height + 20));
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
        this.goal = goal != null && goal.compareTo(BigDecimal.ZERO) > 0 ? goal : BigDecimal.ZERO;
        repaint();
    }

    public void setUnitLabel(String unit) {
        this.unitLabel = unit != null ? unit : "";
        repaint();
    }

    public void setShowDetailedLabels(boolean show) {
        this.showDetailedLabels = show;
        repaint();
    }

    public double getPercentage() {
        if (goal == null || goal.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
        BigDecimal pct = current.multiply(BigDecimal.valueOf(100))
                .divide(goal, 1, RoundingMode.HALF_UP);
        return pct.doubleValue();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int width = getWidth();
        int height = getHeight();
        if (width <= 10) {
            g2.dispose();
            return;
        }

        int actualBarH = Math.min(barHeight, height - (showDetailedLabels ? 18 : 0));
        int barY = 2;

        double pct = getPercentage();
        int fillW = (int) Math.round((Math.min(pct, 100.0) / 100.0) * width);

        // 1. Fundo do trilho
        int arc = Math.min(actualBarH, 16);
        Color trackBg = lightMode ? TRACK_BG_LIGHT : TRACK_BG_DARK;
        Color trackBorder = lightMode ? TRACK_BORDER_LIGHT : TRACK_BORDER_DARK;

        g2.setColor(trackBg);
        g2.fillRoundRect(0, barY, width, actualBarH, arc, arc);

        g2.setColor(trackBorder);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(0, barY, width - 1, actualBarH - 1, arc, arc);

        // 2. Preenchimento gradiente
        if (fillW > 0) {
            Color startColor;
            Color endColor;

            if (pct >= 100.0) {
                // Verde vibrante de meta atingida / superada
                startColor = new Color(16, 185, 129);
                endColor = new Color(5, 150, 105);
            } else if (pct >= 80.0) {
                // Azul safira executivo (próximo da meta)
                startColor = new Color(59, 130, 246);
                endColor = new Color(37, 99, 235);
            } else if (pct >= 50.0) {
                // Âmbar / Amarelo atenção
                startColor = new Color(245, 158, 11);
                endColor = new Color(217, 119, 6);
            } else {
                // Vermelho / Coral alerta
                startColor = new Color(239, 68, 68);
                endColor = new Color(220, 38, 38);
            }

            LinearGradientPaint gradient = new LinearGradientPaint(
                    0, barY, fillW, barY,
                    new float[]{0.0f, 1.0f},
                    new Color[]{startColor, endColor}
            );
            g2.setPaint(gradient);
            g2.fillRoundRect(0, barY, fillW, actualBarH, arc, arc);

            // Borda destacada no preenchimento
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, barY, fillW - 1, actualBarH - 1, arc, arc);
        }

        // 3. Marcadores de metas intermediárias (25%, 50%, 75%)
        g2.setColor(lightMode ? MARKER_LIGHT : MARKER_DARK);
        g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f, 2.0f}, 0.0f));
        for (int p : new int[]{25, 50, 75}) {
            int mx = (int) Math.round((p / 100.0) * width);
            g2.drawLine(mx, barY + 2, mx, barY + actualBarH - 3);
        }

        // 4. Texto de percentagem dentro da barra (sem emojis Unicode)
        String pctStr = String.format("%.1f%%", pct);
        if (pct >= 100.0) {
            pctStr += "  META SUPERADA";
        }

        g2.setFont(new Font(UIHelper.FONT, Font.BOLD, Math.max(12, actualBarH - 12)));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(pctStr);
        int textH = fm.getAscent();

        int textX = (width - textW) / 2;
        int textY = barY + ((actualBarH + textH) / 2) - 3;

        // Se o preenchimento cobre o centro do texto ou modo escuro, usa texto branco com sombra
        if (!lightMode || fillW > (textX + textW / 2)) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(pctStr, textX + 1, textY + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(pctStr, textX, textY);
        } else {
            g2.setColor(new Color(15, 23, 42));
            g2.drawString(pctStr, textX, textY);
        }

        // 5. Legenda inferior com Realizado vs Meta
        if (showDetailedLabels) {
            int labelY = barY + actualBarH + 14;
            g2.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
            FontMetrics fmLbl = g2.getFontMetrics();

            String leftText = String.format("Realizado: %,.2f %s", current, unitLabel);
            g2.setColor(lightMode ? new Color(15, 23, 42) : new Color(203, 213, 225));
            g2.drawString(leftText, 2, labelY);

            String rightText = String.format("Meta: %,.2f %s", goal, unitLabel);
            int rightW = fmLbl.stringWidth(rightText);
            g2.setColor(lightMode ? new Color(100, 116, 139) : new Color(148, 163, 184));
            g2.drawString(rightText, width - rightW - 2, labelY);
        }

        g2.dispose();
    }
}
