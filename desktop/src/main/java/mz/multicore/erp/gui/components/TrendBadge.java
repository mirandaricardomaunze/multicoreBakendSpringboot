package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/** Indicador de tendência micro-pill (ex.: ▲ +8,5%, ▼ -3,2%, — 0,0%) com contraste acessível. */
public class TrendBadge extends JPanel {
    private BigDecimal percent;
    private final JLabel label;
    private boolean onSolidCard;

    public TrendBadge() { this(null, null); }

    public TrendBadge(BigDecimal percent) { this(percent, null); }

    public TrendBadge(BigDecimal percent, String comparisonLabel) {
        setOpaque(false);
        setBorder(new EmptyBorder(1, 6, 1, 6));
        setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        label = new JLabel();
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        add(label);

        updateTrend(percent, comparisonLabel);
    }

    public void updateTrend(BigDecimal percent) {
        updateTrend(percent, null);
    }

    public void updateTrend(BigDecimal percent, String comparisonLabel) {
        this.percent = percent;
        String text;
        Color fg;
        Color bg;

        if (percent == null || percent.compareTo(BigDecimal.ZERO) == 0) {
            text = DashboardTrendCalculator.formatPercentage(percent);
            fg = UIHelper.TEXT_MUTED;
            bg = UIHelper.isLight() ? new Color(241, 245, 249) : new Color(51, 65, 85, 120);
        } else if (percent.compareTo(BigDecimal.ZERO) > 0) {
            text = DashboardTrendCalculator.formatPercentage(percent);
            fg = UIHelper.APPROVED_GREEN;
            bg = new Color(16, 185, 129, 35);
        } else {
            text = DashboardTrendCalculator.formatPercentage(percent);
            fg = UIHelper.REJECTED_RED;
            bg = new Color(239, 68, 68, 35);
        }

        if (onSolidCard) { fg = Color.WHITE; bg = new Color(255, 255, 255, 45); }
        label.setText(text);
        label.setForeground(fg);

        putClientProperty("trend.percent", percent);
        putClientProperty("trend.bg", bg);
        putClientProperty("trend.fg", fg);
        String tip = percent != null ? "Variação: " + text : "Sem variação registada";
        if (comparisonLabel != null && !comparisonLabel.isBlank()) {
            tip += " (" + comparisonLabel + ")";
        }
        setToolTipText(tip);
        revalidate();
        repaint();
    }

    /** Sobre um cartão de cor sólida, verde ou vermelho deixam de se ler: passa a branco. */
    void useOnSolidCard() { onSolidCard = true; updateTrend(percent); }

    public boolean isPositive() {
        return percent != null && percent.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return percent != null && percent.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return percent == null || percent.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public JLabel getLabel() {
        return label;
    }

    public String getText() {
        return label.getText();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = (Color) getClientProperty("trend.bg");
            if (bg != null) {
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
