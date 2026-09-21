package mz.multicore.erp.gui.components;

import mz.multicore.erp.gui.components.ProfitEngine.ProfitMetrics;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Widget executivo de rentabilidade, margem bruta e CMVMC para o Dashboard.
 */
public class ProfitAnalyticsWidget extends ModernPanel {

    private JLabel revenueVal;
    private JLabel cogsVal;
    private JLabel profitVal;
    private JLabel marginBadge;
    private JLabel ticketVal;
    private JProgressBar posBar;
    private JProgressBar invoiceBar;
    private JLabel posLabel;
    private JLabel invoiceLabel;

    public ProfitAnalyticsWidget() {
        super(16);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(16, 18, 16, 18));
        initComponents();
    }

    private void initComponents() {
        // Cabeçalho
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Rentabilidade & Margem Bruta");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        title.setForeground(UIHelper.TEXT_LIGHT);
        title.setIcon(UIHelper.icon("fas-chart-line", 15));

        marginBadge = new JLabel("0.0% Margem");
        marginBadge.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        marginBadge.setOpaque(true);
        marginBadge.setBackground(new Color(16, 185, 129, 40));
        marginBadge.setForeground(UIHelper.APPROVED_GREEN);
        marginBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.APPROVED_GREEN, 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));

        top.add(title, BorderLayout.WEST);
        top.add(marginBadge, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Painel Central com KPIs
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiGrid.setOpaque(false);

        revenueVal = new JLabel("0,00 MT");
        cogsVal = new JLabel("0,00 MT");
        profitVal = new JLabel("0,00 MT");
        ticketVal = new JLabel("0,00 MT");

        kpiGrid.add(createKpiCard("Receita Líquida", revenueVal, UIHelper.ACCENT_BLUE));
        kpiGrid.add(createKpiCard("CMVMC (Custos)", cogsVal, UIHelper.REJECTED_RED));
        kpiGrid.add(createKpiCard("Lucro Bruto", profitVal, UIHelper.APPROVED_GREEN));
        kpiGrid.add(createKpiCard("Ticket Médio", ticketVal, UIHelper.PENDING_YELLOW));

        // Secção de Comparativo de Canais
        JPanel channelsPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        channelsPanel.setOpaque(false);
        channelsPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        posLabel = new JLabel("Vendas POS Balcão: 0,00 MT (0%)");
        posLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        posLabel.setForeground(UIHelper.TEXT_MUTED);
        posBar = createProgressBar(UIHelper.ACCENT_BLUE);

        JPanel posBox = new JPanel(new BorderLayout(0, 3));
        posBox.setOpaque(false);
        posBox.add(posLabel, BorderLayout.NORTH);
        posBox.add(posBar, BorderLayout.CENTER);

        invoiceLabel = new JLabel("Faturação Direta: 0,00 MT (0%)");
        invoiceLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        invoiceLabel.setForeground(UIHelper.TEXT_MUTED);
        invoiceBar = createProgressBar(new Color(139, 92, 246));

        JPanel invBox = new JPanel(new BorderLayout(0, 3));
        invBox.setOpaque(false);
        invBox.add(invoiceLabel, BorderLayout.NORTH);
        invBox.add(invoiceBar, BorderLayout.CENTER);

        channelsPanel.add(posBox);
        channelsPanel.add(invBox);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(kpiGrid, BorderLayout.CENTER);
        center.add(channelsPanel, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
    }

    private JPanel createKpiCard(String label, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setOpaque(true);
        card.setBackground(!UIHelper.isLight() ? new Color(24, 32, 47) : new Color(241, 245, 249));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accentColor),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel title = new JLabel(label);
        title.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        title.setForeground(UIHelper.TEXT_MUTED);

        valueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        valueLabel.setForeground(UIHelper.TEXT_LIGHT);

        card.add(title, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JProgressBar createProgressBar(Color fill) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setPreferredSize(new Dimension(100, 6));
        bar.setForeground(fill);
        bar.setBackground(!UIHelper.isLight() ? new Color(30, 41, 59) : new Color(226, 232, 240));
        bar.setBorderPainted(false);
        return bar;
    }

    public void updateMetrics(ProfitMetrics metrics) {
        if (metrics == null) return;

        revenueVal.setText(String.format("%,.2f MT", metrics.totalRevenue()));
        cogsVal.setText(String.format("%,.2f MT", metrics.totalCogs()));
        profitVal.setText(String.format("%,.2f MT", metrics.grossProfit()));
        ticketVal.setText(String.format("%,.2f MT", metrics.averageTicket()));

        marginBadge.setText(String.format("%.1f%% Margem", metrics.profitMarginPercent()));
        if (metrics.profitMarginPercent().compareTo(new BigDecimal("30.0")) >= 0) {
            marginBadge.setForeground(UIHelper.APPROVED_GREEN);
            marginBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIHelper.APPROVED_GREEN, 1, true),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)
            ));
        } else {
            marginBadge.setForeground(UIHelper.PENDING_YELLOW);
            marginBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIHelper.PENDING_YELLOW, 1, true),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)
            ));
        }

        BigDecimal total = metrics.totalRevenue();
        int posPct = 0;
        int invPct = 0;
        if (total.signum() > 0) {
            posPct = metrics.posRevenue().multiply(BigDecimal.valueOf(100)).divide(total, 0, java.math.RoundingMode.HALF_UP).intValue();
            invPct = 100 - posPct;
        }

        posBar.setValue(posPct);
        posLabel.setText(String.format("Vendas POS Balcão: %,.2f MT (%d%%)", metrics.posRevenue(), posPct));

        invoiceBar.setValue(invPct);
        invoiceLabel.setText(String.format("Faturação Direta: %,.2f MT (%d%%)", metrics.invoiceRevenue(), invPct));
    }
}
