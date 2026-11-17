package mz.multicore.erp.gui.components;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;

/** Section header and pillar cards shared by the strategic and operational pulse widgets. */
final class StrategicPulseCardFactory {

    private StrategicPulseCardFactory() {}

    static JPanel buildHeader(Runnable onRefresh) {
        return buildHeader("fas-compass", "Pulso estratégico",
                "Governação, liquidez, risco e metas em tempo real.", onRefresh);
    }

    /** Title and subtitle share one left edge; refresh is the shared secondary button. */
    static JPanel buildHeader(String iconCode, String titleText, String subtitleText, Runnable onRefresh) {
        JPanel header = new JPanel(new BorderLayout(Spacing.MD, 0));
        header.setOpaque(false);

        JLabel title = new JLabel(titleText, UIHelper.icon(iconCode, 16, UIHelper.ACCENT_BLUE), SwingConstants.LEADING);
        title.setIconTextGap(Spacing.SM);
        title.setFont(Typography.TITLE);
        title.setForeground(UIHelper.TEXT_LIGHT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel(subtitleText);
        sub.setFont(Typography.CAPTION);
        sub.setForeground(UIHelper.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setBorder(new EmptyBorder(Spacing.XXS, 16 + Spacing.SM, 0, 0));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title);
        titleBox.add(sub);
        header.add(titleBox, BorderLayout.CENTER);

        ModernButton refreshBtn = UIHelper.createRefreshButton(onRefresh);
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        east.setOpaque(false);
        east.add(refreshBtn);
        header.add(east, BorderLayout.EAST);
        return header;
    }

    /**
     * Pillar card at its natural height (a fixed height clipped the value under the button). The action
     * is a quiet link in the pillar colour and the whole card opens the same screen.
     */
    static JPanel createPillarCard(String title, String iconCode, Color accentColor,
                                   JLabel valueLabel, JLabel subLabel, ModernButton actionBtn) {
        KpiCard.KpiPalette pal = KpiCard.resolvePalette(accentColor);

        ModernPanel card = new ModernPanel(UIHelper.RADIUS_MD, pal.bg);
        card.putClientProperty("card.border", pal.border);
        card.putClientProperty("kpi.accent", pal.accent);
        card.setLayout(new BorderLayout(0, Spacing.SM));
        card.setBorder(new EmptyBorder(Spacing.MD, Spacing.LG, Spacing.SM, Spacing.LG));

        JLabel titleLbl = new JLabel(title, UIHelper.icon(iconCode, 13, pal.accent), SwingConstants.LEADING);
        titleLbl.setIconTextGap(Spacing.XS);
        titleLbl.setFont(Typography.CAPTION_BOLD);
        titleLbl.setForeground(pal.title);
        card.add(titleLbl, BorderLayout.NORTH);

        valueLabel.setFont(Typography.TITLE);
        valueLabel.setForeground(pal.value);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subLabel.setFont(Typography.CAPTION);
        subLabel.setForeground(pal.subtitle);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(valueLabel);
        content.add(Box.createVerticalStrut(Spacing.XXS));
        content.add(subLabel);
        card.add(content, BorderLayout.CENTER);

        card.add(linkRow(actionBtn, pal), BorderLayout.SOUTH);
        KpiCard.makeInteractive(card, "Abrir " + actionBtn.getText(), actionBtn::doClick);
        return card;
    }

    private static JComponent linkRow(ModernButton actionBtn, KpiCard.KpiPalette pal) {
        actionBtn.setColors(pal.bg, UIHelper.blendColors(pal.bg, pal.accent, 0.10f));
        actionBtn.setForeground(pal.accent);
        actionBtn.setIcon(UIHelper.icon("fas-chevron-right", 10));
        actionBtn.setHorizontalTextPosition(SwingConstants.LEADING);
        actionBtn.setIconTextGap(Spacing.XS);
        actionBtn.setFont(Typography.CAPTION_BOLD);
        actionBtn.setBorder(new EmptyBorder(Spacing.XXS, Spacing.XS, Spacing.XXS, Spacing.XS));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(actionBtn);
        return row;
    }
}
