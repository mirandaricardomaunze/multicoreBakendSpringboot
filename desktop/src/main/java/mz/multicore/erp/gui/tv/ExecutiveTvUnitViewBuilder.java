package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Construtor da visão focada em unidade de negócio específica do painel de TV Executiva.
 */
public final class ExecutiveTvUnitViewBuilder {

    private ExecutiveTvUnitViewBuilder() {}

    public static JPanel build(BusinessUnitDTO bu, List<InvoiceDTO> filteredInvoices,
                               ExecutiveTvDashboardDialog.Period currentPeriod, boolean lightMode) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        BigDecimal unitGoal = BusinessUnitGoalManager.getGoalForUnit(bu.id());
        BigDecimal unitBilled = ExecutiveTvCalculationHelper.calculateTotalBilled(filteredInvoices, bu.id());
        BigDecimal difference = unitBilled.subtract(unitGoal);
        double pct = unitGoal.compareTo(BigDecimal.ZERO) > 0
                ? unitBilled.multiply(BigDecimal.valueOf(100)).divide(unitGoal, 1, RoundingMode.HALF_UP).doubleValue() : 0.0;

        panel.add(buildUnitFocusCard(bu, unitGoal, unitBilled, difference, pct, filteredInvoices, currentPeriod, lightMode), BorderLayout.NORTH);

        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        centerGrid.setOpaque(false);
        centerGrid.add(buildTopProductsTvWidget(filteredInvoices, bu.id(), lightMode));
        centerGrid.add(buildRecentInvoicesTvWidget(filteredInvoices, bu.id(), lightMode));
        panel.add(centerGrid, BorderLayout.CENTER);

        return panel;
    }

    private static JPanel buildUnitFocusCard(BusinessUnitDTO bu, BigDecimal unitGoal, BigDecimal unitBilled,
                                             BigDecimal difference, double pct, List<InvoiceDTO> filteredInvoices,
                                             ExecutiveTvDashboardDialog.Period currentPeriod, boolean lightMode) {
        JPanel focusCard = new ModernPanel(14, ExecutiveTvTheme.getCardBgColor(lightMode), ExecutiveTvTheme.getCardBgColor(lightMode));
        focusCard.setLayout(new BorderLayout(0, 12));
        focusCard.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ExecutiveTvTheme.getCardBorderColor(lightMode), 1, true), new EmptyBorder(16, 20, 16, 20)));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        String supInfo = bu.supplierName() != null ? "  •  Fornecedor: " + bu.supplierName() : "";
        JLabel titleLbl = new JLabel(bu.name().toUpperCase() + supInfo + "  [" + currentPeriod.getLabel().toUpperCase() + "]");
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        titleLbl.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
        titleRow.add(titleLbl, BorderLayout.WEST);
        titleRow.add(ExecutiveTvTheme.buildStatusBadge(pct, lightMode), BorderLayout.EAST);
        focusCard.add(titleRow, BorderLayout.NORTH);

        GoalProgressBar bar = new GoalProgressBar(34);
        bar.setLightMode(lightMode);
        bar.setValues(unitBilled, unitGoal);
        focusCard.add(bar, BorderLayout.CENTER);

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);
        kpiRow.add(ExecutiveTvTheme.buildTvKpi("META DA UNIDADE", String.format("%,.2f MT", unitGoal), "Meta orçamentada", UIHelper.ACCENT_CYAN, lightMode));
        kpiRow.add(ExecutiveTvTheme.buildTvKpi("FATURADO REALIZADO", String.format("%,.2f MT", unitBilled), String.format("%.1f%% alcançado", pct),
                pct >= 100.0 ? UIHelper.APPROVED_GREEN : UIHelper.ACCENT_BLUE, lightMode));

        String diffLbl = difference.compareTo(BigDecimal.ZERO) >= 0 ? "SUPERÁVIT" : "A ATINGIR";
        Color diffCol = difference.compareTo(BigDecimal.ZERO) >= 0 ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW;
        kpiRow.add(ExecutiveTvTheme.buildTvKpi(diffLbl, String.format("%,.2f MT", difference.abs()),
                difference.compareTo(BigDecimal.ZERO) >= 0 ? "Meta ultrapassada" : "Falta faturar", diffCol, lightMode));

        long invoiceCount = filteredInvoices.stream().filter(inv -> bu.id().equals(inv.businessUnitId())).count();
        kpiRow.add(ExecutiveTvTheme.buildTvKpi("FATURAS EMITIDAS", String.valueOf(invoiceCount), "Documentos faturados no período", UIHelper.KPI_PURPLE_SOFT, lightMode));

        focusCard.add(kpiRow, BorderLayout.SOUTH);
        return focusCard;
    }

    public static JPanel buildTopProductsTvWidget(List<InvoiceDTO> filteredInvoices, Long businessUnitId, boolean lightMode) {
        ModernPanel card = new ModernPanel(14, ExecutiveTvTheme.getCardBgColor(lightMode), ExecutiveTvTheme.getCardBgColor(lightMode));
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ExecutiveTvTheme.getCardBorderColor(lightMode), 1, true), new EmptyBorder(12, 14, 12, 14)));

        JLabel titleLbl = new JLabel(businessUnitId == null ? "Top Artigos Campeões (Global)" : "Top Artigos da Unidade");
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        titleLbl.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
        card.add(titleLbl, BorderLayout.NORTH);

        List<ExecutiveTvCalculationHelper.ProductMetric> metrics = ExecutiveTvCalculationHelper.aggregateTopProducts(filteredInvoices, businessUnitId);
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        BigDecimal maxRev = metrics.isEmpty() ? BigDecimal.ONE : metrics.get(0).revenue();
        int rank = 1;
        for (ExecutiveTvCalculationHelper.ProductMetric m : metrics) {
            if (rank > 5) break;
            double pct = maxRev.compareTo(BigDecimal.ZERO) > 0 ? m.revenue().multiply(BigDecimal.valueOf(100)).divide(maxRev, 1, RoundingMode.HALF_UP).doubleValue() : 0.0;
            list.add(createTopProductRow(rank, m.name(), m.revenue(), m.quantity().intValue(), pct, lightMode));
            list.add(Box.createVerticalStrut(6));
            rank++;
        }

        if (metrics.isEmpty()) {
            JLabel emptyLbl = new JLabel("Nenhuma venda no período seleccionado.");
            emptyLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            emptyLbl.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
            list.add(emptyLbl);
        }

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private static JPanel createTopProductRow(int rank, String name, BigDecimal revenue, int qty, double pctOfMax, boolean lightMode) {
        JPanel row = new JPanel(new BorderLayout(6, 3));
        row.setOpaque(false);

        JPanel textRow = new JPanel(new BorderLayout());
        textRow.setOpaque(false);
        JLabel nameLbl = new JLabel(rank + ". " + name);
        nameLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        nameLbl.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));

        JLabel revLbl = new JLabel(String.format("%,.2f MT (%d un)", revenue, qty));
        revLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        revLbl.setForeground(rank == 1 ? UIHelper.PENDING_YELLOW : UIHelper.APPROVED_GREEN);

        textRow.add(nameLbl, BorderLayout.WEST);
        textRow.add(revLbl, BorderLayout.EAST);
        row.add(textRow, BorderLayout.NORTH);

        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(lightMode ? new Color(226, 232, 240) : new Color(51, 65, 85));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                int filled = (int) Math.round((pctOfMax / 100.0) * getWidth());
                if (filled > 0) {
                    g2.setColor(rank == 1 ? UIHelper.PENDING_YELLOW : UIHelper.ACCENT_BLUE);
                    g2.fillRoundRect(0, 0, filled, getHeight(), 4, 4);
                }
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(100, 5));
        row.add(bar, BorderLayout.SOUTH);
        return row;
    }

    private static JPanel buildRecentInvoicesTvWidget(List<InvoiceDTO> filteredInvoices, Long businessUnitId, boolean lightMode) {
        ModernPanel card = new ModernPanel(14, ExecutiveTvTheme.getCardBgColor(lightMode), ExecutiveTvTheme.getCardBgColor(lightMode));
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ExecutiveTvTheme.getCardBorderColor(lightMode), 1, true), new EmptyBorder(12, 14, 12, 14)));

        JLabel titleLbl = new JLabel("Últimas Faturas Emitidas da Linha");
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        titleLbl.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));
        card.add(titleLbl, BorderLayout.NORTH);

        List<InvoiceDTO> unitInvoices = filteredInvoices.stream().filter(inv -> businessUnitId.equals(inv.businessUnitId())).limit(6).toList();
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        for (InvoiceDTO inv : unitInvoices) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(4, 0, 4, 0));

            String client = inv.buyerName() != null ? inv.buyerName() : "Cliente Diversos";
            JLabel invLbl = new JLabel(inv.invoiceNumber() + " • " + client);
            invLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            invLbl.setForeground(ExecutiveTvTheme.getTextLightColor(lightMode));

            JLabel amtLbl = new JLabel(String.format("%,.2f MT", inv.totalAmount()));
            amtLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
            amtLbl.setForeground(UIHelper.APPROVED_GREEN);

            row.add(invLbl, BorderLayout.WEST);
            row.add(amtLbl, BorderLayout.EAST);
            list.add(row);
        }

        if (unitInvoices.isEmpty()) {
            JLabel emptyLbl = new JLabel("Nenhuma fatura registada nesta unidade para o período.");
            emptyLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            emptyLbl.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
            list.add(emptyLbl);
        }

        card.add(list, BorderLayout.CENTER);
        return card;
    }
}
