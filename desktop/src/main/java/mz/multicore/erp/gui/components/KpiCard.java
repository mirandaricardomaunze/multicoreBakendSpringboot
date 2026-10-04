package mz.multicore.erp.gui.components;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Cartão de KPI profissional unificado, com paleta de cores sólida (sem gradientes)
 * e altura espaçosa (96px) para máxima legibilidade de dados em todo o sistema.
 */
public final class KpiCard {

    /** Altura padrão uniforme e espaçosa de todos os cards no sistema. */
    public static final int STANDARD_CARD_HEIGHT = 96;
    public static final int STANDARD_CARD_MIN_WIDTH = 140;

    private KpiCard() {
    }

    /** Label de valor em destaque com o tamanho indicado. */
    public static JLabel valueLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, size));
        l.setForeground(UIHelper.isLight() ? UIHelper.TEXT_LIGHT : Color.WHITE);
        return l;
    }

    /** Grelha padrão para cards de KPI com espaçamento e proporção uniformes. */
    public static JPanel createGrid(int columns) {
        return createGrid(columns, 10, 10);
    }

    public static JPanel createGrid(int columns, int hgap, int vgap) {
        JPanel grid = new JPanel(new GridLayout(0, columns, hgap, vgap));
        grid.setOpaque(false);
        return grid;
    }

    public static final class KpiPalette {
        public final Color bg;
        public final Color border;
        public final Color title;
        public final Color value;
        public final Color subtitle;

        public KpiPalette(Color bg, Color border, Color title, Color value, Color subtitle) {
            this.bg = bg;
            this.border = border;
            this.title = title;
            this.value = value;
            this.subtitle = subtitle;
        }
    }

    public static KpiPalette bluePalette(boolean isLight) {
        Color bg = isLight ? new Color(28, 68, 165) : new Color(22, 54, 138);
        return new KpiPalette(
                bg,
                new Color(96, 165, 250, 180),
                new Color(219, 234, 254),
                Color.WHITE,
                new Color(191, 219, 254)
        );
    }

    public static KpiPalette emeraldPalette(boolean isLight) {
        Color bg = isLight ? new Color(5, 112, 82) : new Color(6, 92, 68);
        return new KpiPalette(
                bg,
                new Color(52, 211, 153, 180),
                new Color(209, 250, 229),
                Color.WHITE,
                new Color(167, 243, 208)
        );
    }

    public static KpiPalette purplePalette(boolean isLight) {
        Color bg = isLight ? new Color(102, 38, 185) : new Color(82, 30, 155);
        return new KpiPalette(
                bg,
                new Color(192, 132, 252, 180),
                new Color(243, 232, 255),
                Color.WHITE,
                new Color(233, 213, 255)
        );
    }

    public static KpiPalette cyanPalette(boolean isLight) {
        Color bg = isLight ? new Color(12, 122, 136) : new Color(11, 102, 115);
        return new KpiPalette(
                bg,
                new Color(34, 211, 238, 180),
                new Color(207, 250, 254),
                Color.WHITE,
                new Color(165, 243, 252)
        );
    }

    public static KpiPalette amberPalette(boolean isLight) {
        Color bg = isLight ? new Color(162, 82, 10) : new Color(138, 68, 8);
        return new KpiPalette(
                bg,
                new Color(251, 191, 36, 180),
                new Color(254, 243, 199),
                Color.WHITE,
                new Color(253, 230, 138)
        );
    }

    public static KpiPalette orangePalette(boolean isLight) {
        Color bg = isLight ? new Color(176, 62, 15) : new Color(152, 52, 12);
        return new KpiPalette(
                bg,
                new Color(251, 146, 60, 180),
                new Color(255, 237, 213),
                Color.WHITE,
                new Color(254, 215, 170)
        );
    }

    public static KpiPalette redPalette(boolean isLight) {
        Color bg = isLight ? new Color(172, 26, 36) : new Color(148, 20, 28);
        return new KpiPalette(
                bg,
                new Color(248, 113, 113, 180),
                new Color(254, 226, 226),
                Color.WHITE,
                new Color(254, 202, 202)
        );
    }

    public static KpiPalette slatePalette(boolean isLight) {
        Color bg = isLight ? new Color(51, 65, 85) : new Color(36, 48, 66);
        return new KpiPalette(
                bg,
                new Color(148, 163, 184, 180),
                new Color(241, 245, 249),
                Color.WHITE,
                new Color(203, 213, 225)
        );
    }

    public static KpiPalette resolvePalette(Color accent) {
        return resolvePalette(accent, null);
    }

    /** Resolve a paleta de cores sólidas e coordenadas por cor de destaque e contexto. */
    public static KpiPalette resolvePalette(Color accent, Color fallbackColor) {
        if (accent == null) {
            accent = fallbackColor != null ? fallbackColor : UIHelper.ACCENT_BLUE;
        }
        boolean isLight = UIHelper.isLight();

        // 1. Mapeamento direto por constantes semânticas do tema
        if (accent.equals(UIHelper.KPI_INFO_SOFT) || accent.equals(UIHelper.KPI_INFO_DARK)
                || accent.equals(UIHelper.ACCENT_BLUE) || accent.equals(UIHelper.ACCENT_SKY)
                || accent.equals(UIHelper.ACCENT_BLUE_HOVER)) {
            return bluePalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_PURPLE_SOFT) || accent.equals(UIHelper.KPI_PURPLE_DARK)
                || accent.equals(UIHelper.KPI_PURPLE_END) || accent.equals(UIHelper.ACCENT)
                || accent.equals(UIHelper.ACCENT_HOVER)) {
            return purplePalette(isLight);
        }
        if (accent.equals(UIHelper.APPROVED_GREEN) || accent.equals(UIHelper.APPROVED_GREEN_HOVER)) {
            return emeraldPalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_SUCCESS_SOFT) || accent.equals(UIHelper.KPI_INFO_END)
                || accent.equals(UIHelper.ACCENT_CYAN)) {
            if (fallbackColor != null && (fallbackColor.equals(UIHelper.APPROVED_GREEN) || fallbackColor.equals(UIHelper.APPROVED_GREEN_HOVER))) {
                return emeraldPalette(isLight);
            }
            return cyanPalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_WARNING_SOFT) || accent.equals(UIHelper.KPI_WARNING_DARK)
                || accent.equals(UIHelper.KPI_WARNING_END) || accent.equals(UIHelper.PENDING_YELLOW)) {
            return amberPalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_ORANGE_SOFT) || accent.equals(UIHelper.KPI_ORANGE_DARK)
                || accent.equals(UIHelper.KPI_ORANGE_END) || accent.equals(UIHelper.ACCENT_ORANGE)) {
            return orangePalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_DANGER_SOFT) || accent.equals(UIHelper.KPI_DANGER_DARK)
                || accent.equals(UIHelper.KPI_DANGER_END) || accent.equals(UIHelper.REJECTED_RED)
                || accent.equals(UIHelper.REJECTED_RED_HOVER)) {
            return redPalette(isLight);
        }
        if (accent.equals(UIHelper.KPI_NEUTRAL_SOFT) || accent.equals(UIHelper.KPI_NEUTRAL_DARK)
                || accent.equals(UIHelper.KPI_NEUTRAL_END) || accent.equals(UIHelper.TEXT_LIGHT)
                || accent.equals(UIHelper.TEXT_MUTED) || accent.equals(UIHelper.BUTTON_NEUTRAL)) {
            return slatePalette(isLight);
        }

        // 2. Análise espectral HSB para cores arbitrárias
        float[] hsb = Color.RGBtoHSB(accent.getRed(), accent.getGreen(), accent.getBlue(), null);
        float hue = hsb[0] * 360f;
        float sat = hsb[1];

        if (sat < 0.18f) {
            return slatePalette(isLight);
        }
        if (hue >= 70 && hue < 160) {
            return emeraldPalette(isLight);
        }
        if (hue >= 160 && hue < 205) {
            return cyanPalette(isLight);
        }
        if (hue >= 205 && hue < 255) {
            return bluePalette(isLight);
        }
        if (hue >= 255 && hue < 325) {
            return purplePalette(isLight);
        }
        if (hue >= 325 || hue < 12) {
            return redPalette(isLight);
        }
        if (hue >= 12 && hue < 42) {
            return orangePalette(isLight);
        }
        return amberPalette(isLight);
    }

    public static ModernPanel create(String title, String iconCode, Color titleColor,
                                     JLabel valueLabel, JLabel subLabel,
                                     Color gradientStart, Color gradientEnd) {
        return create(title, iconCode, titleColor, valueLabel, null, subLabel, gradientStart, gradientEnd);
    }

    public static Color[] resolveKpiGradient(Color accent) {
        KpiPalette pal = resolvePalette(accent);
        return new Color[]{pal.bg, pal.bg};
    }

    public static Color resolveKpiSoftColor(Color accent) {
        return resolvePalette(accent).title;
    }

    public static ModernPanel create(String title, String iconCode, Color titleColor,
                                     JLabel valueLabel, TrendBadge trendBadge, JLabel subLabel,
                                     Color gradientStart, Color gradientEnd) {
        Color primaryAccent = titleColor != null ? titleColor : gradientStart;
        Color secondaryAccent = gradientEnd != null ? gradientEnd : gradientStart;
        KpiPalette pal = resolvePalette(primaryAccent, secondaryAccent);

        ModernPanel card = new ModernPanel(UIHelper.RADIUS_MD, pal.bg);
        card.putClientProperty("card.border", pal.border);
        card.setLayout(new BorderLayout(6, 4));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setPreferredSize(new Dimension(card.getPreferredSize().width, STANDARD_CARD_HEIGHT));
        card.setMinimumSize(new Dimension(STANDARD_CARD_MIN_WIDTH, STANDARD_CARD_HEIGHT));

        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        titleLabel.setForeground(pal.title);
        titleRow.add(titleLabel, BorderLayout.CENTER);
        if (iconCode != null && !iconCode.isBlank()) {
            titleRow.add(new JLabel(UIHelper.icon(iconCode, 18, pal.title)), BorderLayout.EAST);
        }

        card.add(titleRow, BorderLayout.NORTH);

        if (valueLabel != null) {
            valueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 20));
            valueLabel.setForeground(pal.value);
        }

        if (trendBadge != null) {
            JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            centerPanel.setOpaque(false);
            if (valueLabel != null) centerPanel.add(valueLabel);
            centerPanel.add(trendBadge);
            card.add(centerPanel, BorderLayout.CENTER);
        } else if (valueLabel != null) {
            card.add(valueLabel, BorderLayout.CENTER);
        }

        if (subLabel != null) {
            subLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
            subLabel.setForeground(pal.subtitle);
            card.add(subLabel, BorderLayout.SOUTH);
        }
        return card;
    }

    public static ModernPanel createCard(String title, JLabel valueLabel, TrendBadge trendBadge, JLabel subLabel,
                                         String iconName, Color iconColor) {
        KpiPalette pal = resolvePalette(iconColor);
        ModernPanel card = new ModernPanel(UIHelper.RADIUS_MD, pal.bg);
        card.putClientProperty("card.border", pal.border);
        card.setLayout(new BorderLayout(12, 4));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setPreferredSize(new Dimension(card.getPreferredSize().width, STANDARD_CARD_HEIGHT));
        card.setMinimumSize(new Dimension(STANDARD_CARD_MIN_WIDTH, STANDARD_CARD_HEIGHT));

        if (iconName != null && !iconName.isBlank()) {
            JPanel iconBox = new JPanel(new GridBagLayout());
            iconBox.setPreferredSize(new Dimension(40, 40));
            iconBox.setOpaque(false);
            iconBox.add(new JLabel(UIHelper.icon(iconName, 22, pal.title)));
            card.add(iconBox, BorderLayout.WEST);
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        titleLbl.setForeground(pal.title);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(titleLbl);
        textPanel.add(Box.createVerticalStrut(3));

        if (valueLabel != null) {
            valueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 19));
            valueLabel.setForeground(pal.value);
        }

        JPanel valueRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        valueRow.setOpaque(false);
        valueRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (valueLabel != null) valueRow.add(valueLabel);
        if (trendBadge != null) valueRow.add(trendBadge);
        textPanel.add(valueRow);

        if (subLabel != null) {
            subLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
            subLabel.setForeground(pal.subtitle);
            subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(Box.createVerticalStrut(3));
            textPanel.add(subLabel);
        }

        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    public static ModernPanel createCard(String title, JLabel valueLabel, String subtitle, String iconName, Color iconColor) {
        JLabel subLbl = subtitle != null ? new JLabel(subtitle) : null;
        return createCard(title, valueLabel, null, subLbl, iconName, iconColor);
    }

    public static ModernPanel createCard(String title, JLabel valueLabel, String iconName, Color iconColor) {
        return createCard(title, valueLabel, null, null, iconName, iconColor);
    }

    public static ModernPanel createCard(String title, String value, String subtitle, String iconName, Color iconColor) {
        JLabel val = new JLabel(value != null ? value : "—");
        JLabel subLbl = subtitle != null ? new JLabel(subtitle) : null;
        return createCard(title, val, null, subLbl, iconName, iconColor);
    }

    public static ModernPanel createCard(String title, String value, String iconName, Color iconColor) {
        return createCard(title, value, null, iconName, iconColor);
    }

    public static ModernPanel createMetricCard(String title, JLabel valueLabel, JLabel subtitleLabel,
                                               String iconName, Color iconColor, BigDecimal trendPercent) {
        TrendBadge badge = trendPercent != null ? new TrendBadge(trendPercent) : null;
        return createCard(title, valueLabel, badge, subtitleLabel, iconName, iconColor);
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

    /**
     * Torna um cartão de KPI interactivo para acções de drilldown/filtro rápido.
     * Altera o cursor para HAND_CURSOR, adiciona realce subtil de borda no hover
     * e executa a acção ao clicar com o botão esquerdo ou pressionar Enter/Espaço.
     */
    public static ModernPanel makeInteractive(ModernPanel card, String tooltip, Runnable onClickAction) {
        if (card == null || onClickAction == null) return card;
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setFocusable(true);
        if (tooltip != null && !tooltip.isBlank()) {
            card.setToolTipText(tooltip);
        } else {
            card.setToolTipText("Clique para filtrar ou ver detalhes deste indicador");
        }

        Color normalBorder = (Color) card.getClientProperty("card.border");
        Color hoverBorder = normalBorder != null ? normalBorder.brighter() : new Color(255, 255, 255, 140);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.putClientProperty("card.border", hoverBorder);
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.putClientProperty("card.border", normalBorder);
                card.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    onClickAction.run();
                }
            }
        });

        card.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    onClickAction.run();
                }
            }
        });

        return card;
    }

    public static ModernPanel createInteractiveCard(String title, JLabel valueLabel, String subtitle,
                                                    String iconName, Color iconColor,
                                                    String tooltip, Runnable onClickAction) {
        ModernPanel card = createCard(title, valueLabel, subtitle, iconName, iconColor);
        return makeInteractive(card, tooltip, onClickAction);
    }

    public static ModernPanel createInteractiveMetricCard(String title, JLabel valueLabel, String subtitle,
                                                          String iconName, Color iconColor,
                                                          String tooltip, Runnable onClickAction) {
        ModernPanel card = createMetricCard(title, valueLabel, subtitle, iconName, iconColor);
        return makeInteractive(card, tooltip, onClickAction);
    }

    /**
     * Indicador de tendência micro-pill (ex.: ▲ +8.5%, ▼ -3.2%, — 0.0%) com contraste acessível.
     */
    public static class TrendBadge extends JPanel {
        private BigDecimal percent;
        private final JLabel label;

        public TrendBadge() {
            this(null, null);
        }

        public TrendBadge(BigDecimal percent) {
            this(percent, null);
        }

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
                text = "0.0%";
                fg = UIHelper.TEXT_MUTED;
                bg = UIHelper.isLight() ? new Color(241, 245, 249) : new Color(51, 65, 85, 120);
            } else if (percent.compareTo(BigDecimal.ZERO) > 0) {
                text = String.format(Locale.US, "+%.1f%%", percent.doubleValue());
                fg = UIHelper.APPROVED_GREEN;
                bg = new Color(16, 185, 129, 35);
            } else {
                text = String.format(Locale.US, "%.1f%%", percent.doubleValue());
                fg = UIHelper.REJECTED_RED;
                bg = new Color(239, 68, 68, 35);
            }

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
}
