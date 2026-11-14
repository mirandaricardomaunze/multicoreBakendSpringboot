package mz.multicore.erp.gui.components;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;

/**
 * Unified KPI card with a solid colour palette and a consistent 96-pixel height for legibility.
 * {@link #createGrid} lays cards out as a compact strip for screens whose real content is the table below;
 * {@link #createHeroGrid} keeps the full-height cards for dashboards, where the KPI is the content.
 */
public final class KpiCard {

    /** Standard height shared by all KPI cards. */
    public static final int STANDARD_CARD_HEIGHT = 96;
    public static final int STANDARD_CARD_MIN_WIDTH = 140;

    private KpiCard() {}

    /** Creates a prominent value label using the requested font size. */
    public static JLabel valueLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, size));
        l.setForeground(UIHelper.isLight() ? UIHelper.TEXT_LIGHT : Color.WHITE);
        return l;
    }

    /** Compact KPI strip for operational screens: cards added to it shrink so the table keeps the space. */
    public static JPanel createGrid(int columns) {
        return createGrid(columns, 10, 10);
    }

    public static JPanel createGrid(int columns, int hgap, int vgap) {
        return new KpiStrip(columns, hgap, vgap);
    }

    /** Full-height KPI grid for dashboards, where the indicators are the main content. */
    public static JPanel createHeroGrid(int columns, int hgap, int vgap) {
        JPanel grid = new JPanel(new GridLayout(0, columns, hgap, vgap));
        grid.setOpaque(false);
        return grid;
    }

    public static final class KpiPalette {
        public final Color bg, border, title, value, subtitle, accent;

        public KpiPalette(Color bg, Color border, Color title, Color value, Color subtitle, Color accent) {
            this.bg = bg;
            this.border = border;
            this.title = title;
            this.value = value;
            this.subtitle = subtitle;
            this.accent = accent;
        }
    }

    public static KpiPalette redPalette(boolean isLight) { return KpiCardPalette.redPalette(isLight); }
    public static KpiPalette slatePalette(boolean isLight) { return KpiCardPalette.slatePalette(isLight); }
    public static KpiPalette resolvePalette(Color accent) { return KpiCardPalette.resolvePalette(accent); }
    public static KpiPalette resolvePalette(Color accent, Color fallbackColor) { return KpiCardPalette.resolvePalette(accent, fallbackColor); }

    public static ModernPanel create(String title, String iconCode, Color titleColor,
                                     JLabel valueLabel, JLabel subLabel,
                                     Color gradientStart, Color gradientEnd) {
        return create(title, iconCode, titleColor, valueLabel, null, subLabel, gradientStart, gradientEnd);
    }

    public static ModernPanel create(String title, String iconCode, Color titleColor,
                                     JLabel valueLabel, TrendBadge trendBadge, JLabel subLabel,
                                     Color gradientStart, Color gradientEnd) {
        Color primaryAccent = titleColor != null ? titleColor : gradientStart;
        Color secondaryAccent = gradientEnd != null ? gradientEnd : gradientStart;
        KpiPalette pal = resolvePalette(primaryAccent, secondaryAccent);

        ModernPanel card = shell(pal, 6);

        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        titleLabel.setForeground(pal.title);
        titleRow.add(titleLabel, BorderLayout.CENTER);
        JLabel iconLabel = iconCode == null || iconCode.isBlank() ? null : new JLabel(UIHelper.icon(iconCode, 18, pal.title));
        if (iconLabel != null) titleRow.add(iconLabel, BorderLayout.EAST);

        card.add(titleRow, BorderLayout.NORTH);

        if (valueLabel != null) {
            valueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 20));
            valueLabel.setForeground(pal.value);
        }

        if (trendBadge != null) {
            JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); // sem recuo: valor alinhado com os cartões sem pílula
            centerPanel.setOpaque(false);
            if (valueLabel != null) centerPanel.add(valueLabel);
            centerPanel.add(Box.createHorizontalStrut(8)); centerPanel.add(trendBadge);
            card.add(centerPanel, BorderLayout.CENTER);
        } else if (valueLabel != null) {
            card.add(valueLabel, BorderLayout.CENTER);
        }

        if (subLabel != null) {
            subLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
            subLabel.setForeground(pal.subtitle);
            card.add(subLabel, BorderLayout.SOUTH);
        }
        KpiAccent.install(card, valueLabel, iconCode, pal);
        KpiCompactLayout.remember(card, new KpiCompactLayout.Parts(titleLabel, iconCode, iconLabel, valueLabel, trendBadge, subLabel));
        return KpiTextFit.wire(card, null, valueLabel, trendBadge, subLabel, false);
    }

    public static ModernPanel createCard(String title, JLabel valueLabel, TrendBadge trendBadge, JLabel subLabel,
                                         String iconName, Color iconColor) {
        KpiPalette pal = resolvePalette(iconColor);
        ModernPanel card = shell(pal, 12);

        JLabel iconLabel = null;
        if (iconName != null && !iconName.isBlank()) {
            JPanel iconBox = new JPanel(new GridBagLayout());
            iconBox.setPreferredSize(new Dimension(40, 40));
            iconBox.setOpaque(false);
            iconBox.add(iconLabel = new JLabel(UIHelper.icon(iconName, 22, pal.title)));
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

        JPanel valueRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        valueRow.setOpaque(false);
        valueRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (valueLabel != null) valueRow.add(valueLabel);
        if (trendBadge != null) { valueRow.add(Box.createHorizontalStrut(8)); valueRow.add(trendBadge); }
        textPanel.add(valueRow);

        if (subLabel != null) {
            subLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
            subLabel.setForeground(pal.subtitle);
            subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(Box.createVerticalStrut(3));
            textPanel.add(subLabel);
        }

        card.add(textPanel, BorderLayout.CENTER);
        KpiAccent.install(card, valueLabel, iconName, pal);
        KpiCompactLayout.remember(card, new KpiCompactLayout.Parts(titleLbl, iconName, iconLabel, valueLabel, trendBadge, subLabel));
        return KpiTextFit.wire(card, titleLbl, valueLabel, trendBadge, subLabel, iconName != null && !iconName.isBlank());
    }

    private static ModernPanel shell(KpiPalette pal, int hgap) {
        ModernPanel card = new ModernPanel(UIHelper.RADIUS_MD, pal.bg);
        card.putClientProperty("card.border", pal.border);
        card.setLayout(new BorderLayout(hgap, 4));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setPreferredSize(new Dimension(card.getPreferredSize().width, STANDARD_CARD_HEIGHT));
        card.setMinimumSize(new Dimension(STANDARD_CARD_MIN_WIDTH, STANDARD_CARD_HEIGHT));
        return card;
    }

    public static ModernPanel createCard(String title, JLabel valueLabel, String subtitle, String iconName, Color iconColor) {
        return createCard(title, valueLabel, null, subtitle != null ? new JLabel(subtitle) : null, iconName, iconColor);
    }

    public static ModernPanel createCard(String title, JLabel valueLabel, String iconName, Color iconColor) {
        return createCard(title, valueLabel, null, null, iconName, iconColor);
    }

    public static ModernPanel createCard(String title, String value, String subtitle, String iconName, Color iconColor) {
        return createCard(title, new JLabel(value != null ? value : "—"), null, subtitle != null ? new JLabel(subtitle) : null, iconName, iconColor);
    }

    public static ModernPanel createCard(String title, String value, String iconName, Color iconColor) {
        return createCard(title, value, null, iconName, iconColor);
    }

    public static ModernPanel createMetricCard(String title, JLabel valueLabel, JLabel subtitleLabel,
                                               String iconName, Color iconColor, BigDecimal trendPercent) {
        return createCard(title, valueLabel, trendPercent != null ? new TrendBadge(trendPercent) : null, subtitleLabel, iconName, iconColor);
    }

    public static ModernPanel createMetricCard(String title, JLabel valueLabel, JLabel subtitleLabel, String iconName, Color iconColor) {
        return createMetricCard(title, valueLabel, subtitleLabel, iconName, iconColor, null);
    }

    public static ModernPanel createMetricCard(String title, JLabel valueLabel, String subtitle, String iconName, Color iconColor) {
        return createMetricCard(title, valueLabel, subtitle != null ? new JLabel(subtitle) : null, iconName, iconColor, null);
    }

    public static ModernPanel createMetricCard(String title, JLabel valueLabel, String subtitle, String iconName, Color iconColor, BigDecimal trendPercent) {
        return createMetricCard(title, valueLabel, subtitle != null ? new JLabel(subtitle) : null, iconName, iconColor, trendPercent);
    }

    public static ModernPanel createMetricCard(String title, String value, String subtitle, String iconName, Color iconColor) {
        return createMetricCard(title, new JLabel(value != null ? value : "—"), subtitle, iconName, iconColor, null);
    }

    public static ModernPanel createMetricCard(String title, String value, String subtitle, String iconName, Color iconColor, BigDecimal trendPercent) {
        return createMetricCard(title, new JLabel(value != null ? value : "—"), subtitle, iconName, iconColor, trendPercent);
    }

    public static ModernPanel makeInteractive(ModernPanel card, String tooltip, Runnable onClickAction) {
        return KpiCardInteractiveHelper.makeInteractive(card, tooltip, onClickAction);
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

    public static class TrendBadge extends mz.multicore.erp.gui.components.TrendBadge {
        public TrendBadge() { super(); }
        public TrendBadge(BigDecimal percent) { super(percent); }
        public TrendBadge(BigDecimal percent, String comparisonLabel) { super(percent, comparisonLabel); }
    }
}
