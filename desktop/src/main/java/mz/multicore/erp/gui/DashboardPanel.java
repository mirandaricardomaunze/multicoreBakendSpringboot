package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.ArrowScrollPanel;
import mz.multicore.erp.gui.components.DashboardTrendCalculator;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ProfitAnalyticsWidget;
import mz.multicore.erp.gui.components.ProfitEngine;
import mz.multicore.erp.gui.components.RecentActivityWidget;
import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.SimplePieChart;
import mz.multicore.erp.gui.components.StrategicPulseWidget;
import mz.multicore.erp.gui.components.TopProductsWidget;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.CRMApiClient;
import mz.multicore.erp.desktop.client.CashFlowForecastApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditRiskApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.ForensicAuditApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceLineDTO;
import mz.multicore.erp.modules.comercial.dto.POSSalesSummaryDTO;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;

import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel {

    public enum PeriodFilter {
        HOJE("Hoje"),
        ESTA_SEMANA("Esta Semana"),
        ESTE_MES("Este Mês"),
        ESTE_ANO("Este Ano"),
        TODOS("Todo o Período");

        private final String label;
        PeriodFilter(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final ComercialApiClient comercialApiClient;
    private final FinanceApiClient financeApiClient;
    private final ApprovalApiClient approvalApiClient;
    private final CRMApiClient crmApiClient;
    private final PurchaseApiClient purchaseApiClient;
    private final InventoryApiClient inventoryApiClient;
    private final Consumer<String> navigationHandler;

    private PeriodFilter currentPeriod = PeriodFilter.HOJE;
    private final Map<PeriodFilter, ModernButton> periodButtons = new HashMap<>();

    private JLabel welcomeLabel;
    private JLabel balanceValLabel;
    private JLabel salesValLabel;
    private KpiCard.TrendBadge salesTrendBadge;
    private JLabel salesSubLabel;
    private JLabel posSalesValLabel;
    private KpiCard.TrendBadge posTrendBadge;
    private JLabel posSalesCountSub;
    private JLabel approvalsValLabel;
    private JLabel ticketsValLabel;
    private JLabel taxSummaryLabel;
    private JLabel taxDetailLabel;
    private JLabel stockAlertsLabel;
    private JLabel expiryAlertsLabel;
    private JLabel expiryAlertsSub;

    /** Horizonte (dias) do alerta de validade no dashboard. */
    private static final int EXPIRY_ALERT_DAYS = 30;
    private SimpleBarChart financialChart;
    private SimpleBarChart operationsChart;
    private SimplePieChart salesChannelPieChart;
    private SimplePieChart financialStructurePieChart;
    private ProfitAnalyticsWidget profitAnalyticsWidget;
    private TopProductsWidget topProductsWidget;
    private RecentActivityWidget recentActivityWidget;
    private final StrategicPulseWidget strategicPulseWidget;

    public DashboardPanel(
            ComercialApiClient comercialApiClient,
            FinanceApiClient financeApiClient,
            ApprovalApiClient approvalApiClient,
            CRMApiClient crmApiClient,
            PurchaseApiClient purchaseApiClient,
            InventoryApiClient inventoryApiClient
    ) {
        this(comercialApiClient, financeApiClient, approvalApiClient, crmApiClient, purchaseApiClient, inventoryApiClient,
                null, null, null, null, null);
    }

    public DashboardPanel(
            ComercialApiClient comercialApiClient,
            FinanceApiClient financeApiClient,
            ApprovalApiClient approvalApiClient,
            CRMApiClient crmApiClient,
            PurchaseApiClient purchaseApiClient,
            InventoryApiClient inventoryApiClient,
            ForensicAuditApiClient forensicAuditApiClient,
            CashFlowForecastApiClient cashFlowForecastApiClient,
            CreditRiskApiClient creditRiskApiClient,
            PerformanceApiClient performanceApiClient,
            Consumer<String> navigationHandler
    ) {
        this.comercialApiClient = comercialApiClient;
        this.financeApiClient = financeApiClient;
        this.approvalApiClient = approvalApiClient;
        this.crmApiClient = crmApiClient;
        this.purchaseApiClient = purchaseApiClient;
        this.inventoryApiClient = inventoryApiClient;
        this.navigationHandler = navigationHandler;
        this.strategicPulseWidget = new StrategicPulseWidget(
                forensicAuditApiClient,
                cashFlowForecastApiClient,
                creditRiskApiClient,
                performanceApiClient,
                navigationHandler
        );

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 22, 20, 22));

        // Header Panel: Title (Left) + Period Filter Chips (Right)
        JPanel headerPanel = new JPanel(new BorderLayout(12, 6));
        headerPanel.setOpaque(false);

        JPanel titlesStack = new JPanel();
        titlesStack.setOpaque(false);
        titlesStack.setLayout(new BoxLayout(titlesStack, BoxLayout.Y_AXIS));

        welcomeLabel = new JLabel("Olá, Gestor! Bem-vindo ao MULTICORE.");
        welcomeLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 22));
        welcomeLabel.setForeground(UIHelper.TEXT_LIGHT);
        titlesStack.add(welcomeLabel);

        JLabel subtitle = new JLabel("Visão geral das operações, vendas e fluxo financeiro.");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        titlesStack.add(subtitle);

        headerPanel.add(titlesStack, BorderLayout.WEST);
        headerPanel.add(buildPeriodFilterBar(), BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        JPanel dashboardContent = new JPanel(new BorderLayout(0, 16));
        dashboardContent.setOpaque(false);
        dashboardContent.setBorder(new EmptyBorder(14, 0, 0, 0));

        // Grelha de 4 colunas de KPIs compactos e elegantes
        JPanel gridPanel = new JPanel(new GridLayout(0, 4, 10, 10));
        gridPanel.setOpaque(false);

        balanceValLabel = newValueLabel("0.00 MT", 19);
        gridPanel.add(buildKpiCard(
                "SALDO DE TESOURARIA", "fas-piggy-bank", UIHelper.KPI_INFO_SOFT,
                balanceValLabel, null,
                UIHelper.KPI_INFO_DARK, UIHelper.KPI_INFO_END,
                "Clique para abrir módulo Financeiro / Tesouraria", "financeiro"));

        salesValLabel = newValueLabel("0.00 MT", 19);
        salesTrendBadge = new KpiCard.TrendBadge(null);
        salesSubLabel = new JLabel("Ticket Médio: 0.00 MT/venda");
        salesSubLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        salesSubLabel.setForeground(UIHelper.KPI_PURPLE_SOFT);
        gridPanel.add(buildKpiCard(
                "FATURAÇÃO TOTAL", "fas-file-invoice-dollar", UIHelper.KPI_PURPLE_SOFT,
                salesValLabel, salesTrendBadge, salesSubLabel,
                UIHelper.KPI_PURPLE_DARK, UIHelper.KPI_PURPLE_END,
                "Clique para abrir módulo Comercial / Faturação", "comercial"));

        posSalesValLabel = newValueLabel("0.00 MT", 19);
        posTrendBadge = new KpiCard.TrendBadge(null);
        posSalesCountSub = new JLabel("0 vendas");
        posSalesCountSub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        posSalesCountSub.setForeground(UIHelper.KPI_SUCCESS_SOFT);
        gridPanel.add(buildKpiCard(
                "VENDAS POS", "fas-cash-register", UIHelper.KPI_SUCCESS_SOFT,
                posSalesValLabel, posTrendBadge, posSalesCountSub,
                UIHelper.KPI_INFO_END, UIHelper.APPROVED_GREEN,
                "Clique para abrir Ponto de Venda (POS)", "pos"));

        taxSummaryLabel = newValueLabel("0.00 MT", 19);
        taxDetailLabel = new JLabel("IVA Liquidado / Deduzido");
        taxDetailLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        taxDetailLabel.setForeground(UIHelper.KPI_SUCCESS_SOFT);
        gridPanel.add(buildKpiCard(
                "RESUMO FISCAL DO IVA", "fas-percentage", UIHelper.KPI_SUCCESS_SOFT,
                taxSummaryLabel, taxDetailLabel,
                UIHelper.KPI_INFO_DARK, UIHelper.KPI_INFO_END,
                "Clique para abrir Área Fiscal", "fiscal"));

        approvalsValLabel = newValueLabel("0 Pedidos", 19);
        gridPanel.add(buildKpiCard(
                "APROVAÇÕES PENDENTES", "fas-clipboard-check", UIHelper.KPI_WARNING_SOFT,
                approvalsValLabel, null,
                UIHelper.KPI_WARNING_DARK, UIHelper.KPI_WARNING_END,
                "Clique para abrir Aprovações", "approvals"));

        ticketsValLabel = newValueLabel("0 Tickets", 19);
        gridPanel.add(buildKpiCard(
                "SUPORTE CRM / ASSISTÊNCIAS", "fas-headset", UIHelper.KPI_NEUTRAL_SOFT,
                ticketsValLabel, null,
                UIHelper.KPI_NEUTRAL_DARK, UIHelper.KPI_NEUTRAL_END,
                "Clique para abrir CRM & Assistência", "crm"));

        stockAlertsLabel = newValueLabel("0 Artigos", 19);
        JLabel stockAlertsSub = new JLabel("Qtd < 5 un no armazém");
        stockAlertsSub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        stockAlertsSub.setForeground(UIHelper.KPI_DANGER_SOFT);
        gridPanel.add(buildKpiCard(
                "ALERTAS DE STOCK", "fas-exclamation-triangle", UIHelper.KPI_DANGER_SOFT,
                stockAlertsLabel, stockAlertsSub,
                UIHelper.KPI_DANGER_DARK, UIHelper.KPI_DANGER_END,
                "Clique para abrir Gestão de Stock", "stock"));

        expiryAlertsLabel = newValueLabel("0 Lotes", 19);
        expiryAlertsSub = new JLabel("Vencimento ≤ " + EXPIRY_ALERT_DAYS + " dias");
        expiryAlertsSub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        expiryAlertsSub.setForeground(UIHelper.KPI_ORANGE_SOFT);
        gridPanel.add(buildKpiCard(
                "ALERTAS DE VALIDADE", "fas-calendar-times", UIHelper.KPI_ORANGE_SOFT,
                expiryAlertsLabel, expiryAlertsSub,
                UIHelper.KPI_ORANGE_DARK, UIHelper.KPI_ORANGE_END,
                "Clique para abrir Gestão de Lotes / Stock", "stock"));

        dashboardContent.add(gridPanel, BorderLayout.NORTH);

        // Painel central: Pulso Estratégico 360° + Gráficos 2x2 + Widgets de Ranking e Atividade Recente
        JPanel centerContainer = new JPanel();
        centerContainer.setOpaque(false);
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));

        centerContainer.add(strategicPulseWidget);
        centerContainer.add(Box.createVerticalStrut(14));

        // Gráficos 2x2
        JPanel chartsPanel = new JPanel(new GridLayout(2, 2, 14, 14));
        chartsPanel.setOpaque(false);

        financialChart = new SimpleBarChart("Vendas, Compras e IVA");
        salesChannelPieChart = new SimplePieChart("Vendas por Canal (POS vs Faturas)", false);
        operationsChart = new SimpleBarChart("Operações e Pendências");
        financialStructurePieChart = new SimplePieChart("Estrutura Financeira (Receita, Compras, IVA)", false);

        chartsPanel.add(createChartCard(financialChart));
        chartsPanel.add(createChartCard(salesChannelPieChart));
        chartsPanel.add(createChartCard(operationsChart));
        chartsPanel.add(createChartCard(financialStructurePieChart));
        centerContainer.add(chartsPanel);

        centerContainer.add(Box.createVerticalStrut(14));

        profitAnalyticsWidget = new ProfitAnalyticsWidget();
        centerContainer.add(profitAnalyticsWidget);

        centerContainer.add(Box.createVerticalStrut(14));

        // Widgets Inferiores: Top 5 Produtos + Atividade Recente
        JPanel bottomWidgets = new JPanel(new GridLayout(1, 2, 14, 14));
        bottomWidgets.setOpaque(false);

        topProductsWidget = new TopProductsWidget();
        recentActivityWidget = new RecentActivityWidget();

        bottomWidgets.add(topProductsWidget);
        bottomWidgets.add(recentActivityWidget);
        centerContainer.add(bottomWidgets);

        dashboardContent.add(centerContainer, BorderLayout.CENTER);

        // Scroll wrapper fluido com botões de seta superior/inferior e largura adaptativa (sem overflow)
        ArrowScrollPanel scroll = new ArrowScrollPanel(dashboardContent);
        add(scroll, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel buildPeriodFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        bar.setOpaque(false);

        for (PeriodFilter p : PeriodFilter.values()) {
            ModernButton btn = UIHelper.createSecondaryButton(p.getLabel());
            btn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 12, 28));
            btn.addActionListener(e -> selectPeriod(p));
            periodButtons.put(p, btn);
            bar.add(btn);
        }
        updatePeriodButtonStyles();
        return bar;
    }

    public void selectPeriod(PeriodFilter period) {
        if (period == null) return;
        this.currentPeriod = period;
        updatePeriodButtonStyles();
        refreshData();
    }

    public PeriodFilter getCurrentPeriod() {
        return currentPeriod;
    }

    private void updatePeriodButtonStyles() {
        for (Map.Entry<PeriodFilter, ModernButton> entry : periodButtons.entrySet()) {
            boolean active = entry.getKey() == currentPeriod;
            ModernButton btn = entry.getValue();
            if (active) {
                btn.setColors(UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
                btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            } else {
                Color idleBg = UIHelper.isLight() ? UIHelper.ROW_ALT : UIHelper.BG_CARD;
                Color idleHover = UIHelper.isLight() ? UIHelper.GRID : UIHelper.ROW_ALT;
                btn.setColors(idleBg, idleHover);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIHelper.BORDER, 1, true),
                        BorderFactory.createEmptyBorder(3, 9, 3, 9)
                ));
            }
        }
    }

    private JLabel newValueLabel(String text, int size) {
        return KpiCard.valueLabel(text, size);
    }

    private ModernPanel buildKpiCard(String title, String iconCode, Color titleColor,
                                      JLabel valueLabel, JLabel subLabel,
                                      Color gradientStart, Color gradientEnd) {
        return buildKpiCard(title, iconCode, titleColor, valueLabel, subLabel, gradientStart, gradientEnd, null, null);
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
                                      Color gradientStart, Color gradientEnd) {
        return buildKpiCard(title, iconCode, titleColor, valueLabel, trendBadge, subLabel, gradientStart, gradientEnd, null, null);
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

    public void updateWelcomeMessage(String username, String role) {
        welcomeLabel.setText("Olá, " + username);
    }

    public void refreshData() {
        if (strategicPulseWidget != null) {
            strategicPulseWidget.loadData();
        }

        if (financeApiClient == null || comercialApiClient == null || approvalApiClient == null || crmApiClient == null || purchaseApiClient == null || inventoryApiClient == null) {
            return;
        }

        UIHelper.loadAsync(this, this::fetchDashboardData, this::applyDashboardData, error -> {
            putClientProperty("loadError", error.getMessage());
            setToolTipText("Não foi possível actualizar o dashboard: " + error.getMessage());
        });
    }

    private DashboardData fetchDashboardData() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        LocalDate today = LocalDate.now();

        DashboardTrendCalculator.ComparisonPeriod comp =
                DashboardTrendCalculator.resolvePeriods(currentPeriod, today);
        DashboardTrendCalculator.DateRange curRange = comp.current();
        DashboardTrendCalculator.DateRange prevRange = comp.previous();
        String compLabel = comp.comparisonLabel();

        LocalDate fromDate = curRange.from();
        LocalDate toDate = curRange.to();

        // 1. Saldo de tesouraria
        BigDecimal totalBal = financeApiClient.getAllAccounts().stream()
                .map(TreasuryAccountDTO::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Faturação comercial
        List<InvoiceDTO> allInvoices = comercialApiClient.getAllInvoices();
        List<InvoiceDTO> filteredInvoices = allInvoices.stream()
                .filter(i -> i.status() == InvoiceStatus.APPROVED || i.status() == InvoiceStatus.PAID)
                .filter(i -> curRange.contains(i.createdAt() != null ? i.createdAt().toLocalDate() : null))
                .toList();

        BigDecimal totalSales = filteredInvoices.stream()
                .map(InvoiceDTO::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal prevSales = BigDecimal.ZERO;
        if (prevRange != null) {
            prevSales = allInvoices.stream()
                    .filter(i -> i.status() == InvoiceStatus.APPROVED || i.status() == InvoiceStatus.PAID)
                    .filter(i -> prevRange.contains(i.createdAt() != null ? i.createdAt().toLocalDate() : null))
                    .map(InvoiceDTO::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal salesTrendPercent = (prevRange != null)
                ? DashboardTrendCalculator.calculatePercentageChange(totalSales, prevSales)
                : null;
        BigDecimal salesAvgTicket = DashboardTrendCalculator.calculateAverageTicket(totalSales, filteredInvoices.size());

        // 3. Vendas POS
        POSSalesSummaryDTO posSummary = null;
        POSSalesSummaryDTO posPrevSummary = null;
        POSSalesSummaryDTO posToday = null;
        try {
            posSummary = comercialApiClient.getPOSSalesSummary(companyId, fromDate, toDate);
            if (prevRange != null) {
                posPrevSummary = comercialApiClient.getPOSSalesSummary(companyId, prevRange.from(), prevRange.to());
            }
            posToday = comercialApiClient.getPOSSalesSummary(companyId, today, today);
        } catch (Exception ignored) {}

        BigDecimal posSalesAmount = posSummary != null && posSummary.totalAmount() != null ? posSummary.totalAmount() : BigDecimal.ZERO;
        long posSalesCount = posSummary != null ? posSummary.count() : 0L;
        BigDecimal posPrevAmount = posPrevSummary != null && posPrevSummary.totalAmount() != null ? posPrevSummary.totalAmount() : BigDecimal.ZERO;
        BigDecimal posTrendPercent = (prevRange != null)
                ? DashboardTrendCalculator.calculatePercentageChange(posSalesAmount, posPrevAmount)
                : null;
        BigDecimal posTodayTotal = posToday != null && posToday.totalAmount() != null ? posToday.totalAmount() : BigDecimal.ZERO;
        long posTodayCount = posToday != null ? posToday.count() : 0L;
        BigDecimal posAvgTicket = DashboardTrendCalculator.calculateAverageTicket(posSalesAmount, posSalesCount);

        // 4. Pendências e CRM
        int appCount = approvalApiClient.getPendingRequests().size();
        long ticketCount = crmApiClient.getAllTickets().stream()
                .filter(t -> "OPEN".equals(t.status()))
                .count();

        // 5. IVA e Compras
        BigDecimal ivaLiquidado = filteredInvoices.stream()
                .map(InvoiceDTO::taxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PurchaseDTO> allPurchases = purchaseApiClient.getPurchasesByCompany(companyId);
        List<PurchaseDTO> filteredPurchases = allPurchases.stream()
                .filter(p -> !"CANCELLED".equals(p.status()))
                .filter(p -> isDateInRange(p.purchaseDate() != null ? p.purchaseDate().toLocalDate() : null, fromDate, toDate))
                .toList();

        BigDecimal ivaDeduzido = filteredPurchases.stream()
                .map(PurchaseDTO::taxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPurchases = filteredPurchases.stream()
                .map(PurchaseDTO::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ivaLiquido = ivaLiquidado.subtract(ivaDeduzido);

        // 6. Stock e Validades
        List<StockDTO> companyStocks = inventoryApiClient.getStocksByCompany(companyId);
        long lowStocksCount = companyStocks.stream()
                .filter(s -> s.quantity().compareTo(BigDecimal.valueOf(5)) < 0)
                .count();

        List<mz.multicore.erp.modules.inventory.dto.ProductBatchDTO> expiring =
                inventoryApiClient.findExpiringBatches(companyId, EXPIRY_ALERT_DAYS);
        long expiredCount = expiring.stream()
                .filter(b -> b.expirationDate() != null && b.expirationDate().isBefore(today))
                .count();
        long soonCount = expiring.size() - expiredCount;

        // 7. Top 5 Produtos Mais Vendidos
        Map<String, ProductSalesAccumulator> productMap = new HashMap<>();
        for (InvoiceDTO inv : filteredInvoices) {
            if (inv.lines() != null) {
                for (InvoiceLineDTO line : inv.lines()) {
                    String name = line.productName() != null ? line.productName() : "Artigo";
                    ProductSalesAccumulator acc = productMap.computeIfAbsent(name, k -> new ProductSalesAccumulator(name));
                    acc.add(line.lineTotal(), line.quantity() != null ? line.quantity().intValue() : 1);
                }
            }
        }

        BigDecimal sumProductRevenue = productMap.values().stream()
                .map(ProductSalesAccumulator::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TopProductsWidget.RankedProduct> topRanked = productMap.values().stream()
                .sorted(Comparator.comparing(ProductSalesAccumulator::getTotalRevenue).reversed())
                .limit(5)
                .map(p -> {
                    double pct = sumProductRevenue.compareTo(BigDecimal.ZERO) > 0
                            ? p.getTotalRevenue().multiply(BigDecimal.valueOf(100)).divide(sumProductRevenue, 1, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return new TopProductsWidget.RankedProduct(p.name, p.totalRevenue, p.totalQuantity, pct);
                })
                .toList();

        // 8. Atividades Recentes
        List<RecentActivityWidget.ActivityEntry> activities = new ArrayList<>();
        int actIdx = 1;
        for (InvoiceDTO inv : allInvoices.stream().limit(4).toList()) {
            activities.add(new RecentActivityWidget.ActivityEntry(
                    "inv_" + (actIdx++),
                    "Fatura " + inv.invoiceNumber() + " (" + inv.status() + ")",
                    "Total: " + String.format("%,.2f MT", inv.totalAmount()) + " · " + (inv.clientName() != null ? inv.clientName() : "Consumidor"),
                    inv.createdAt() != null ? inv.createdAt().format(DateTimeFormatter.ofPattern("dd/MM")) : "Hoje",
                    "fas-file-invoice-dollar",
                    UIHelper.ACCENT_BLUE
            ));
        }

        for (PurchaseDTO pur : allPurchases.stream().limit(3).toList()) {
            activities.add(new RecentActivityWidget.ActivityEntry(
                    "pur_" + (actIdx++),
                    "Compra " + pur.purchaseNumber(),
                    "Fornecedor: " + (pur.supplierName() != null ? pur.supplierName() : "Geral") + " · " + String.format("%,.2f MT", pur.totalAmount()),
                    pur.purchaseDate() != null ? pur.purchaseDate().format(DateTimeFormatter.ofPattern("dd/MM")) : "Hoje",
                    "fas-shopping-cart",
                    UIHelper.APPROVED_GREEN
            ));
        }

        // 9. Rentabilidade e Margens (DRE)
        ProfitEngine.ProfitMetrics profitMetrics = ProfitEngine.calculateMetrics(filteredInvoices, posSalesAmount, null);

        return new DashboardData(totalBal, totalSales, posSalesAmount, posTodayTotal, posTodayCount,
                salesTrendPercent, posTrendPercent, compLabel, salesAvgTicket, posAvgTicket,
                appCount, ticketCount, ivaLiquidado, ivaDeduzido, ivaLiquido, totalPurchases,
                lowStocksCount, expiring.size(), expiredCount, soonCount, topRanked, activities, profitMetrics);
    }

    private static boolean isDateInRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return true;
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    private void applyDashboardData(DashboardData data) {
        balanceValLabel.setText(String.format("%,.2f MT", data.totalBalance()));
        salesValLabel.setText(String.format("%,.2f MT", data.totalSales()));
        salesTrendBadge.updateTrend(data.salesTrendPercent(), data.compLabel());
        salesSubLabel.setText("Ticket Médio: " + DashboardTrendCalculator.formatAverageTicket(data.salesAvgTicket()));

        posSalesValLabel.setText(String.format("%,.2f MT", data.posSalesAmount()));
        posTrendBadge.updateTrend(data.posTrendPercent(), data.compLabel());
        if (currentPeriod == PeriodFilter.HOJE) {
            posSalesCountSub.setText(String.format("%d venda%s hoje · TM: %,.2f MT",
                    data.posTodayCount(), data.posTodayCount() == 1 ? "" : "s", data.posAvgTicket()));
        } else {
            posSalesCountSub.setText(String.format("%d venda%s · TM: %,.2f MT",
                    data.posTodayCount(), data.posTodayCount() == 1 ? "" : "s", data.posAvgTicket()));
        }

        approvalsValLabel.setText(data.approvalCount() + " Pedidos");
        ticketsValLabel.setText(data.ticketCount() + " Abertos");
        String labelPrefix = data.netVat().compareTo(BigDecimal.ZERO) >= 0 ? "IVA a Pagar: " : "IVA a Recuperar: ";
        taxSummaryLabel.setText(labelPrefix + String.format("%,.2f MT", data.netVat().abs()));
        taxDetailLabel.setText(String.format("Liq: %,.2f | Ded: %,.2f MT", data.outputVat(), data.inputVat()));
        stockAlertsLabel.setText(data.lowStockCount() + " Artigo" + (data.lowStockCount() == 1 ? "" : "s"));
        expiryAlertsLabel.setText(data.expiringCount() + " Lote" + (data.expiringCount() == 1 ? "" : "s"));
        expiryAlertsSub.setText(String.format("%d vencido%s · %d a vencer",
                data.expiredCount(), data.expiredCount() == 1 ? "" : "s", data.soonCount()));

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
                new String[]{"Aprov.", "Tickets", "Stock"},
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
        profitAnalyticsWidget.updateMetrics(data.profitMetrics());
    }

    private static class ProductSalesAccumulator {
        private final String name;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private int totalQuantity = 0;

        ProductSalesAccumulator(String name) { this.name = name; }
        void add(BigDecimal rev, int qty) {
            if (rev != null) totalRevenue = totalRevenue.add(rev);
            totalQuantity += qty;
        }
        BigDecimal getTotalRevenue() { return totalRevenue; }
    }

    public StrategicPulseWidget getStrategicPulseWidget() {
        return strategicPulseWidget;
    }

    private record DashboardData(BigDecimal totalBalance, BigDecimal totalSales,
                                 BigDecimal posSalesAmount, BigDecimal posTodayTotal, long posTodayCount,
                                 BigDecimal salesTrendPercent, BigDecimal posTrendPercent, String compLabel,
                                 BigDecimal salesAvgTicket, BigDecimal posAvgTicket,
                                 int approvalCount, long ticketCount, BigDecimal outputVat,
                                 BigDecimal inputVat, BigDecimal netVat, BigDecimal totalPurchases,
                                 long lowStockCount, int expiringCount, long expiredCount, long soonCount,
                                 List<TopProductsWidget.RankedProduct> topProducts,
                                 List<RecentActivityWidget.ActivityEntry> recentActivities,
                                 ProfitEngine.ProfitMetrics profitMetrics) {}

}
