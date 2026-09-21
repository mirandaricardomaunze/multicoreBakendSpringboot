package mz.multicore.erp.gui.components;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;

/**
 * Cartão de KPI profissional (gradiente + ícone), partilhado entre os painéis de visão geral
 * (Dashboard, RH, …). Cabeçalho com título e ícone, valor em destaque ao centro e uma linha de
 * detalhe opcional em baixo. Uma fonte de verdade para o aspecto dos KPIs.
 */
public final class KpiCard {

    private KpiCard() {
    }

    /** Label de valor em destaque (branco, negrito) com o tamanho indicado. */
    public static JLabel valueLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, size));
        l.setForeground(Color.WHITE);
        return l;
    }

    public static ModernPanel create(String title, String iconCode, Color titleColor,
                                     JLabel valueLabel, JLabel subLabel,
                                     Color gradientStart, Color gradientEnd) {
        ModernPanel card = new ModernPanel(UIHelper.RADIUS_MD, gradientStart, gradientEnd);
        card.setLayout(new BorderLayout(6, 6));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        titleLabel.setForeground(titleColor);
        titleRow.add(titleLabel, BorderLayout.CENTER);
        titleRow.add(new JLabel(UIHelper.icon(iconCode, 18, titleColor)), BorderLayout.EAST);

        card.add(titleRow, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        if (subLabel != null) {
            card.add(subLabel, BorderLayout.SOUTH);
        }
        return card;
    }

    /**
     * Indicador de tendência micro-pill (ex.: ▲ +8.5%, ▼ -3.2%, — 0.0%) com contraste acessível.
     */
    public static class TrendBadge extends JPanel {
        private final BigDecimal percent;
        private final JLabel label;

        public TrendBadge(BigDecimal percent) {
            this.percent = percent;
            setOpaque(false);
            setBorder(new EmptyBorder(1, 6, 1, 6));
            setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

            String text;
            Color fg;
            Color bg;

            if (percent == null || percent.compareTo(BigDecimal.ZERO) == 0) {
                text = "— 0.0%";
                fg = UIHelper.TEXT_MUTED;
                bg = UIHelper.isLight() ? new Color(241, 245, 249) : new Color(51, 65, 85, 120);
            } else if (percent.compareTo(BigDecimal.ZERO) > 0) {
                text = String.format(java.util.Locale.US, "▲ +%.1f%%", percent.doubleValue());
                fg = UIHelper.APPROVED_GREEN;
                bg = new Color(16, 185, 129, 35);
            } else {
                text = String.format(java.util.Locale.US, "▼ %.1f%%", percent.doubleValue());
                fg = UIHelper.REJECTED_RED;
                bg = new Color(239, 68, 68, 35);
            }

            label = new JLabel(text);
            label.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
            label.setForeground(fg);
            add(label);

            putClientProperty("trend.percent", percent);
            putClientProperty("trend.bg", bg);
            putClientProperty("trend.fg", fg);
            setToolTipText(percent != null ? "Variação: " + text : "Sem variação registada");
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
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
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

    /**
     * Cartão de KPI executivo com ícone à esquerda em caixa e pilha vertical de textos
     * (título, valor em destaque com indicador de tendência opcional e subtítulo contextual).
     * Padronização canónica reutilizável em todos os painéis e centros executivos.
     */
    public static ModernPanel createMetricCard(String title, JLabel valueLabel, JLabel subtitleLabel,
                                             String iconName, Color iconColor, BigDecimal trendPercent) {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(12, 6));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel iconBox = new JPanel(new java.awt.GridBagLayout());
        iconBox.setPreferredSize(new java.awt.Dimension(42, 42));
        iconBox.setOpaque(false);
        JLabel icon = new JLabel(UIHelper.icon(iconName, 22, iconColor));
        iconBox.add(icon);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new javax.swing.BoxLayout(textPanel, javax.swing.BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        titleLbl.setForeground(UIHelper.TEXT_MUTED);
        titleLbl.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        valueLabel.setForeground(UIHelper.TEXT_LIGHT);

        JPanel valueRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        valueRow.setOpaque(false);
        valueRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        valueRow.add(valueLabel);
        if (trendPercent != null) {
            TrendBadge badge = new TrendBadge(trendPercent);
            valueRow.add(badge);
        }

        if (subtitleLabel != null) {
            subtitleLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
            subtitleLabel.setForeground(UIHelper.TEXT_MUTED);
            subtitleLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        }

        textPanel.add(titleLbl);
        textPanel.add(javax.swing.Box.createVerticalStrut(2));
        textPanel.add(valueRow);
        textPanel.add(javax.swing.Box.createVerticalStrut(2));
        if (subtitleLabel != null) {
            textPanel.add(subtitleLabel);
        }

        card.add(iconBox, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    public static ModernPanel createMetricCard(String title, JLabel valueLabel, JLabel subtitleLabel, String iconName, Color iconColor) {
        return createMetricCard(title, valueLabel, subtitleLabel, iconName, iconColor, null);
    }

    public static ModernPanel createMetricCard(String title, JLabel valueLabel, String subtitle, String iconName, Color iconColor) {
        JLabel subLbl = subtitle != null ? new JLabel(subtitle) : null;
        return createMetricCard(title, valueLabel, subLbl, iconName, iconColor, null);
    }

    public static ModernPanel createMetricCard(String title, JLabel valueLabel, String subtitle, String iconName, Color iconColor, BigDecimal trendPercent) {
        JLabel subLbl = subtitle != null ? new JLabel(subtitle) : null;
        return createMetricCard(title, valueLabel, subLbl, iconName, iconColor, trendPercent);
    }

    public static ModernPanel createMetricCard(String title, String value, String subtitle, String iconName, Color iconColor) {
        JLabel val = new JLabel(value != null ? value : "—");
        return createMetricCard(title, val, subtitle, iconName, iconColor, null);
    }

    public static ModernPanel createMetricCard(String title, String value, String subtitle, String iconName, Color iconColor, BigDecimal trendPercent) {
        JLabel val = new JLabel(value != null ? value : "—");
        return createMetricCard(title, val, subtitle, iconName, iconColor, trendPercent);
    }
}
