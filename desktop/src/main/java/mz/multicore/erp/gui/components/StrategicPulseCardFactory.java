package mz.multicore.erp.gui.components;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

final class StrategicPulseCardFactory {

    private StrategicPulseCardFactory() {}

    static JPanel buildHeader(Runnable onRefresh) {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        JLabel icon = new JLabel(UIHelper.icon("fas-compass", 18, UIHelper.ACCENT_BLUE));
        JLabel title = new JLabel("PULSO ESTRATÉGICO DA EMPRESA (DECISÃO 360°)");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        title.setForeground(UIHelper.TEXT_LIGHT);
        titleRow.add(icon);
        titleRow.add(title);
        titleBox.add(titleRow);

        JLabel sub = new JLabel("Indicadores em tempo real de Governação, Liquidez, Risco e Metas.");
        sub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        sub.setForeground(UIHelper.TEXT_MUTED);
        sub.setBorder(new EmptyBorder(3, 26, 0, 0));
        titleBox.add(sub);

        header.add(titleBox, BorderLayout.WEST);

        ModernButton refreshBtn = UIHelper.createRefreshButton(onRefresh);
        refreshBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        refreshBtn.setPreferredSize(new Dimension(135, 28));
        header.add(refreshBtn, BorderLayout.EAST);

        return header;
    }

    static JPanel createPillarCard(
            String title,
            String iconCode,
            Color accentColor,
            JLabel valueLabel,
            JLabel subLabel,
            ModernButton actionBtn,
            String actionIcon
    ) {
        KpiCard.KpiPalette pal = KpiCard.resolvePalette(accentColor);

        ModernPanel card = new ModernPanel(UIHelper.RADIUS_MD, pal.bg);
        card.putClientProperty("card.border", pal.border);
        card.putClientProperty("kpi.accent", pal.accent);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.setPreferredSize(new Dimension(card.getPreferredSize().width, KpiCard.STANDARD_CARD_HEIGHT));
        card.setMinimumSize(new Dimension(KpiCard.STANDARD_CARD_MIN_WIDTH, KpiCard.STANDARD_CARD_HEIGHT));

        JPanel cardHeader = new JPanel(new BorderLayout(6, 0));
        cardHeader.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        titleLbl.setForeground(pal.title);

        JLabel iconLbl = new JLabel(UIHelper.icon(iconCode, 14, pal.accent));
        cardHeader.add(iconLbl, BorderLayout.WEST);
        cardHeader.add(titleLbl, BorderLayout.CENTER);
        card.add(cardHeader, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        valueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        valueLabel.setForeground(pal.value);
        valueLabel.setAlignmentX(0.0f);

        subLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        subLabel.setForeground(pal.subtitle);
        subLabel.setAlignmentX(0.0f);

        content.add(valueLabel);
        content.add(Box.createVerticalStrut(3));
        content.add(subLabel);
        card.add(content, BorderLayout.CENTER);

        actionBtn.setIcon(UIHelper.icon(actionIcon, 11));
        actionBtn.setPreferredSize(new Dimension(0, 26));
        actionBtn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        card.add(actionBtn, BorderLayout.SOUTH);

        return card;
    }
}
