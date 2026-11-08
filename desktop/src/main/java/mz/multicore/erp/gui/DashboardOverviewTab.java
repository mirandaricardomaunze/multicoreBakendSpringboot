package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.function.Consumer;

/** Dashboard tab containing the overview, commercial KPIs, charts, and operational widgets. */
final class DashboardOverviewTab {

    private static final String NEXT_STEP_HINT = "Registe uma venda no POS ou escolha outro período.";
    static final String NO_PENDING_MESSAGE = "Tudo em dia";

    private final Consumer<String> navigationHandler;

    private JLabel balanceValLabel;
    private JLabel salesValLabel;
    private KpiCard.TrendBadge salesTrendBadge;
    private JLabel salesSubLabel;
    private JLabel posSalesValLabel;
    private KpiCard.TrendBadge posTrendBadge;
    private JLabel posSalesCountSub;
    private JLabel taxSummaryLabel;
    private JLabel taxDetailLabel;

    private SimpleBarChart financialChart;
    private SimpleBarChart operationsChart;
    private SimplePieChart salesChannelPieChart;
    private SimplePieChart financialStructurePieChart;
    JPanel financialRow;
    private TopProductsWidget topProductsWidget;
    private RecentActivityWidget recentActivityWidget;

    DashboardOverviewTab(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
    }

    public JPanel buildTab() {
        JPanel tab1 = new JPanel(new BorderLayout(0, 12));
        tab1.setOpaque(false);
        tab1.setBorder(new EmptyBorder(12, 4, 4, 4));

        JPanel commercialKpiGrid = KpiCard.createGrid(4, 10, 10);

        balanceValLabel = KpiCard.valueLabel("0,00 MT", 19);
        // The balance is always current; the caption prevents it from appearing period-filtered.
        commercialKpiGrid.add(buildKpiCard("SALDO DE TESOURARIA", "fas-piggy-bank", UIHelper.KPI_INFO_SOFT,
                balanceValLabel, new JLabel("Saldo actual de caixa e bancos"), UIHelper.KPI_INFO_DARK, UIHelper.KPI_INFO_END,
                "Clique para abrir módulo Financeiro / Tesouraria", "financeiro"));

        salesValLabel = KpiCard.valueLabel("0,00 MT", 19);
        salesTrendBadge = new KpiCard.TrendBadge(null);
        salesSubLabel = new JLabel("Valor médio: 0,00 MT/venda");
        salesSubLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        salesSubLabel.setForeground(UIHelper.KPI_PURPLE_SOFT);
        commercialKpiGrid.add(buildKpiCard("FATURAÇÃO TOTAL", "fas-file-invoice-dollar", UIHelper.KPI_PURPLE_SOFT,
                salesValLabel, salesTrendBadge, salesSubLabel,
                UIHelper.KPI_PURPLE_DARK, UIHelper.KPI_PURPLE_END,
                "Clique para abrir módulo Comercial / Faturação", "comercial"));

        posSalesValLabel = KpiCard.valueLabel("0,00 MT", 19);
        posTrendBadge = new KpiCard.TrendBadge(null);
        posSalesCountSub = new JLabel("0 vendas");
        posSalesCountSub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        posSalesCountSub.setForeground(UIHelper.KPI_SUCCESS_SOFT);
        commercialKpiGrid.add(buildKpiCard("VENDAS POS", "fas-cash-register", UIHelper.KPI_SUCCESS_SOFT,
                posSalesValLabel, posTrendBadge, posSalesCountSub,
                UIHelper.KPI_INFO_END, UIHelper.APPROVED_GREEN,
                "Clique para abrir Ponto de Venda (POS)", "pos"));

        taxSummaryLabel = KpiCard.valueLabel("0,00 MT", 19);
        taxDetailLabel = new JLabel("A pagar · Liquidado: 0,00 | Dedutível: 0,00 MT");
        taxDetailLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        taxDetailLabel.setForeground(UIHelper.KPI_SUCCESS_SOFT);
        commercialKpiGrid.add(buildKpiCard("IVA DO PERÍODO", "fas-percentage", UIHelper.KPI_SUCCESS_SOFT,
                taxSummaryLabel, taxDetailLabel,
                UIHelper.KPI_INFO_DARK, UIHelper.KPI_INFO_END,
                "Clique para abrir Área Fiscal", "fiscal"));

        tab1.add(commercialKpiGrid, BorderLayout.NORTH);

        JPanel tab1Center = new JPanel();
        tab1Center.setOpaque(false);
        tab1Center.setLayout(new BoxLayout(tab1Center, BoxLayout.Y_AXIS));

        financialChart = new SimpleBarChart("Vendas, Compras e IVA");
        salesChannelPieChart = new SimplePieChart("Vendas por Canal (POS vs Faturas)", false);
        operationsChart = new SimpleBarChart("Operações e Pendências");
        operationsChart.setEmptyMessage(NO_PENDING_MESSAGE);
        operationsChart.setEmptyHint("Sem aprovações, tickets ou stock baixo por tratar.");
        operationsChart.setEmptyIcon(UIHelper.icon("fas-check-circle", 30, UIHelper.APPROVED_GREEN));
        financialStructurePieChart = new SimplePieChart("Estrutura Financeira (Receita, Compras, IVA)", false);
        financialChart.setEmptyHint(NEXT_STEP_HINT);
        salesChannelPieChart.setEmptyHint(NEXT_STEP_HINT);
        financialStructurePieChart.setEmptyHint(NEXT_STEP_HINT);

        // Pending items come first because they represent work the manager can resolve today.
        JPanel priorityRow = new JPanel(new GridLayout(1, 2, 12, 12));
        priorityRow.setOpaque(false);
        priorityRow.add(createChartCard(operationsChart));
        priorityRow.add(createChartCard(financialChart));
        tab1Center.add(priorityRow);

        financialRow = new JPanel(new GridLayout(1, 2, 12, 12));
        financialRow.setOpaque(false);
        financialRow.setBorder(new EmptyBorder(12, 0, 0, 0));
        financialRow.add(createChartCard(salesChannelPieChart));
        financialRow.add(createChartCard(financialStructurePieChart));
        tab1Center.add(financialRow);

        tab1Center.add(Box.createVerticalStrut(12));

        JPanel bottomWidgets = new JPanel(new GridLayout(1, 2, 12, 12));
        bottomWidgets.setOpaque(false);
        topProductsWidget = new TopProductsWidget();
        recentActivityWidget = new RecentActivityWidget();
        bottomWidgets.add(topProductsWidget);
        bottomWidgets.add(recentActivityWidget);
        tab1Center.add(bottomWidgets);

        tab1.add(new ArrowScrollPanel(tab1Center), BorderLayout.CENTER);
        return tab1;
    }

    private ModernPanel buildKpiCard(String title, String iconCode, Color titleColor,
                                     JLabel valueLabel, JLabel subLabel,
                                     Color gradientStart, Color gradientEnd,
                                     String tooltip, String targetModule) {
        ModernPanel card = KpiCard.create(title, iconCode, titleColor, valueLabel, subLabel, gradientStart, gradientEnd);
        if (navigationHandler != null && targetModule != null) {
            KpiCard.makeInteractive(card, tooltip, () -> navigationHandler.accept(targetModule));
        }
        return card;
    }

    private ModernPanel buildKpiCard(String title, String iconCode, Color titleColor,
                                     JLabel valueLabel, KpiCard.TrendBadge trendBadge, JLabel subLabel,
                                     Color gradientStart, Color gradientEnd,
                                     String tooltip, String targetModule) {
        ModernPanel card = KpiCard.create(title, iconCode, titleColor, valueLabel, trendBadge, subLabel, gradientStart, gradientEnd);
        if (navigationHandler != null && targetModule != null) {
            KpiCard.makeInteractive(card, tooltip, () -> navigationHandler.accept(targetModule));
        }
        return card;
    }

    private ModernPanel createChartCard(JComponent chart) {
        ModernPanel card = new ModernPanel(12, UIHelper.BG_CARD, UIHelper.BG_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    void applyData(DashboardDataCoordinator.DashboardData data) {
        balanceValLabel.setText(String.format("%,.2f MT", data.totalBalance()));
        salesValLabel.setText(String.format("%,.2f MT", data.totalSales()));
        boolean comparable = DashboardTrendCalculator.hasComparison(data.compLabel());
        salesTrendBadge.updateTrend(data.salesTrendPercent(), data.compLabel());
        salesTrendBadge.setVisible(comparable);
        salesSubLabel.setText(DashboardTrendCalculator.activityHint(data.totalSales(), data.previousSales(), data.compLabel(),
                "Valor médio: " + DashboardTrendCalculator.formatAverageTicket(data.salesAvgTicket())));

        posSalesValLabel.setText(String.format("%,.2f MT", data.posSalesAmount()));
        posTrendBadge.updateTrend(data.posTrendPercent(), data.compLabel());
        posTrendBadge.setVisible(comparable);
        posSalesCountSub.setText(DashboardTrendCalculator.activityHint(data.posSalesAmount(), data.previousPosSales(), data.compLabel(),
                String.format("%d venda%s · média: %,.2f MT", data.posSalesCount(), data.posSalesCount() == 1 ? "" : "s", data.posAvgTicket())));

        // Keep the value consistent with other cards by placing its direction in the caption.
        String direction = data.netVat().compareTo(BigDecimal.ZERO) >= 0 ? "A pagar" : "A recuperar";
        taxSummaryLabel.setText(String.format("%,.2f MT", data.netVat().abs()));
        taxDetailLabel.setText(String.format("%s · Liquidado: %,.2f | Dedutível: %,.2f MT", direction, data.outputVat(), data.inputVat()));

        financialRow.setVisible(SimpleBarChart.hasData(
                new BigDecimal[]{data.totalSales(), data.totalPurchases(), data.posSalesAmount()}));

        financialChart.setData(
                new String[]{"Vendas", "Compras", "IVA"},
                new BigDecimal[]{data.totalSales(), data.totalPurchases(), data.netVat().abs()},
                new Color[]{UIHelper.ACCENT_BLUE, UIHelper.APPROVED_GREEN, UIHelper.PENDING_YELLOW}
        );

        BigDecimal directCommercialSales = data.totalSales().subtract(data.posSalesAmount());
        if (directCommercialSales.signum() < 0) directCommercialSales = BigDecimal.ZERO;

        salesChannelPieChart.setData(
                new String[]{"Balcão POS", "Faturas Comerciais"},
                new BigDecimal[]{data.posSalesAmount(), directCommercialSales},
                new Color[]{UIHelper.APPROVED_GREEN, UIHelper.ACCENT_BLUE}
        );

        operationsChart.setData(
                new String[]{"Aprovações", "Tickets", "Stock baixo"},
                new BigDecimal[]{
                        BigDecimal.valueOf(data.approvalCount()),
                        BigDecimal.valueOf(data.ticketCount()),
                        BigDecimal.valueOf(data.lowStockCount())
                },
                new Color[]{UIHelper.PENDING_YELLOW, UIHelper.ACCENT, UIHelper.REJECTED_RED}
        );

        financialStructurePieChart.setData(
                new String[]{"Receitas Vendas", "Compras Stock", "IVA Líquido"},
                new BigDecimal[]{data.totalSales(), data.totalPurchases(), data.netVat().abs()},
                new Color[]{UIHelper.ACCENT_BLUE, UIHelper.REJECTED_RED, UIHelper.PENDING_YELLOW}
        );

        topProductsWidget.setProducts(data.topProducts());
        recentActivityWidget.setActivities(data.recentActivities());
    }
}
