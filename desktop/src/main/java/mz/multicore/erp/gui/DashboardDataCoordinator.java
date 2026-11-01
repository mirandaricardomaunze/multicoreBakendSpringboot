package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.DashboardTrendCalculator;
import mz.multicore.erp.gui.components.ProfitEngine;
import mz.multicore.erp.gui.components.RecentActivityWidget;
import mz.multicore.erp.gui.components.TopProductsWidget;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceLineDTO;
import mz.multicore.erp.modules.comercial.dto.POSSalesSummaryDTO;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Coordenador de recolha e agregação de dados assíncronos do Dashboard. */
final class DashboardDataCoordinator {

    static final int EXPIRY_ALERT_DAYS = 30;

    private final ComercialApiClient comercialApiClient;
    private final FinanceApiClient financeApiClient;
    private final ApprovalApiClient approvalApiClient;
    private final CRMApiClient crmApiClient;
    private final PurchaseApiClient purchaseApiClient;
    private final InventoryApiClient inventoryApiClient;

    DashboardDataCoordinator(
            ComercialApiClient comercialApiClient,
            FinanceApiClient financeApiClient,
            ApprovalApiClient approvalApiClient,
            CRMApiClient crmApiClient,
            PurchaseApiClient purchaseApiClient,
            InventoryApiClient inventoryApiClient
    ) {
        this.comercialApiClient = comercialApiClient;
        this.financeApiClient = financeApiClient;
        this.approvalApiClient = approvalApiClient;
        this.crmApiClient = crmApiClient;
        this.purchaseApiClient = purchaseApiClient;
        this.inventoryApiClient = inventoryApiClient;
    }

    DashboardData fetchDashboardData(DashboardPanel.PeriodFilter currentPeriod) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        LocalDate today = LocalDate.now();

        var comp = DashboardTrendCalculator.resolvePeriods(currentPeriod, today);
        var curRange = comp.current();
        var prevRange = comp.previous();
        String compLabel = comp.comparisonLabel();

        BigDecimal totalBal = financeApiClient.getAllAccounts().stream()
                .map(TreasuryAccountDTO::balance).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InvoiceDTO> allInvoices = comercialApiClient.getAllInvoices();
        List<InvoiceDTO> filteredInvoices = allInvoices.stream()
                .filter(i -> i.status() == InvoiceStatus.APPROVED || i.status() == InvoiceStatus.PAID)
                .filter(i -> curRange.contains(i.createdAt() != null ? i.createdAt().toLocalDate() : null))
                .toList();

        BigDecimal totalSales = filteredInvoices.stream().map(InvoiceDTO::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevSales = prevRange == null ? BigDecimal.ZERO : allInvoices.stream()
                .filter(i -> i.status() == InvoiceStatus.APPROVED || i.status() == InvoiceStatus.PAID)
                .filter(i -> prevRange.contains(i.createdAt() != null ? i.createdAt().toLocalDate() : null))
                .map(InvoiceDTO::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal salesTrend = prevRange != null ? DashboardTrendCalculator.calculatePercentageChange(totalSales, prevSales) : null;
        BigDecimal salesAvgTicket = DashboardTrendCalculator.calculateAverageTicket(totalSales, filteredInvoices.size());

        POSSalesSummaryDTO posSummary = null, posPrevSummary = null, posToday = null;
        try {
            posSummary = comercialApiClient.getPOSSalesSummary(companyId, curRange.from(), curRange.to());
            if (prevRange != null) posPrevSummary = comercialApiClient.getPOSSalesSummary(companyId, prevRange.from(), prevRange.to());
            posToday = comercialApiClient.getPOSSalesSummary(companyId, today, today);
        } catch (Exception ignored) {}

        BigDecimal posSales = posSummary != null && posSummary.totalAmount() != null ? posSummary.totalAmount() : BigDecimal.ZERO;
        long posSalesCount = posSummary != null ? posSummary.count() : 0L;
        BigDecimal posPrevAmount = posPrevSummary != null && posPrevSummary.totalAmount() != null ? posPrevSummary.totalAmount() : BigDecimal.ZERO;
        BigDecimal posTrend = prevRange != null ? DashboardTrendCalculator.calculatePercentageChange(posSales, posPrevAmount) : null;
        BigDecimal posTodayTotal = posToday != null && posToday.totalAmount() != null ? posToday.totalAmount() : BigDecimal.ZERO;
        BigDecimal posAvgTicket = DashboardTrendCalculator.calculateAverageTicket(posSales, posSalesCount);

        int appCount = approvalApiClient.getPendingRequests().size();
        long ticketCount = crmApiClient.getAllTickets().stream().filter(t -> "OPEN".equals(t.status())).count();

        BigDecimal ivaLiquidado = filteredInvoices.stream().map(InvoiceDTO::taxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PurchaseDTO> allPurchases = purchaseApiClient.getPurchasesByCompany(companyId);
        List<PurchaseDTO> filteredPurchases = allPurchases.stream()
                .filter(p -> !"CANCELLED".equals(p.status()))
                .filter(p -> isDateInRange(p.purchaseDate() != null ? p.purchaseDate().toLocalDate() : null, curRange.from(), curRange.to()))
                .toList();

        BigDecimal ivaDeduzido = filteredPurchases.stream().map(PurchaseDTO::taxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPurchases = filteredPurchases.stream().map(PurchaseDTO::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ivaLiquido = ivaLiquidado.subtract(ivaDeduzido);

        List<StockDTO> companyStocks = inventoryApiClient.getStocksByCompany(companyId);
        long lowStocksCount = companyStocks.stream().filter(s -> s.quantity().compareTo(BigDecimal.valueOf(5)) < 0).count();

        List<ProductBatchDTO> expiring = inventoryApiClient.findExpiringBatches(companyId, EXPIRY_ALERT_DAYS);
        long expiredCount = expiring.stream().filter(b -> b.expirationDate() != null && b.expirationDate().isBefore(today)).count();
        long soonCount = expiring.size() - expiredCount;

        List<TopProductsWidget.RankedProduct> topRanked = calculateTopProducts(filteredInvoices);
        List<RecentActivityWidget.ActivityEntry> activities = buildRecentActivities(allInvoices, allPurchases);
        ProfitEngine.ProfitMetrics profitMetrics = ProfitEngine.calculateMetrics(filteredInvoices, posSales, null);

        return new DashboardData(totalBal, totalSales, posSales, posTodayTotal, posSalesCount, prevSales, posPrevAmount,
                salesTrend, posTrend, compLabel, salesAvgTicket, posAvgTicket,
                appCount, ticketCount, ivaLiquidado, ivaDeduzido, ivaLiquido, totalPurchases,
                lowStocksCount, expiring.size(), expiredCount, soonCount, topRanked, activities, profitMetrics);
    }

    private static List<TopProductsWidget.RankedProduct> calculateTopProducts(List<InvoiceDTO> filteredInvoices) {
        Map<String, ProductSalesAccumulator> map = new HashMap<>();
        for (InvoiceDTO inv : filteredInvoices) {
            if (inv.lines() != null) {
                for (InvoiceLineDTO line : inv.lines()) {
                    String name = line.productName() != null ? line.productName() : "Artigo";
                    map.computeIfAbsent(name, ProductSalesAccumulator::new)
                            .add(line.lineTotal(), line.quantity() != null ? line.quantity().intValue() : 1);
                }
            }
        }
        BigDecimal sumRev = map.values().stream().map(ProductSalesAccumulator::getTotalRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return map.values().stream()
                .sorted(Comparator.comparing(ProductSalesAccumulator::getTotalRevenue).reversed())
                .limit(5)
                .map(p -> {
                    double pct = sumRev.compareTo(BigDecimal.ZERO) > 0
                            ? p.getTotalRevenue().multiply(BigDecimal.valueOf(100)).divide(sumRev, 1, RoundingMode.HALF_UP).doubleValue() : 0.0;
                    return new TopProductsWidget.RankedProduct(p.name, p.totalRevenue, p.totalQuantity, pct);
                }).toList();
    }

    private static List<RecentActivityWidget.ActivityEntry> buildRecentActivities(List<InvoiceDTO> invoices, List<PurchaseDTO> purchases) {
        List<RecentActivityWidget.ActivityEntry> list = new ArrayList<>();
        int idx = 1;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (InvoiceDTO inv : invoices.stream().limit(4).toList()) {
            list.add(new RecentActivityWidget.ActivityEntry(
                    "inv_" + (idx++), "Fatura " + inv.invoiceNumber() + " (" + inv.status() + ")",
                    "Total: " + String.format("%,.2f MT", inv.totalAmount()) + " · " + (inv.clientName() != null ? inv.clientName() : "Consumidor"),
                    inv.createdAt() != null ? inv.createdAt().format(fmt) : "Hoje", "fas-file-invoice-dollar", UIHelper.ACCENT_BLUE));
        }
        for (PurchaseDTO pur : purchases.stream().limit(3).toList()) {
            list.add(new RecentActivityWidget.ActivityEntry(
                    "pur_" + (idx++), "Compra " + pur.purchaseNumber(),
                    "Fornecedor: " + (pur.supplierName() != null ? pur.supplierName() : "Geral") + " · " + String.format("%,.2f MT", pur.totalAmount()),
                    pur.purchaseDate() != null ? pur.purchaseDate().format(fmt) : "Hoje", "fas-shopping-cart", UIHelper.APPROVED_GREEN));
        }
        return list;
    }

    private static boolean isDateInRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return true;
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
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

    record DashboardData(BigDecimal totalBalance, BigDecimal totalSales,
                         BigDecimal posSalesAmount, BigDecimal posTodayTotal, long posSalesCount,
                         BigDecimal previousSales, BigDecimal previousPosSales,
                         BigDecimal salesTrendPercent, BigDecimal posTrendPercent, String compLabel,
                         BigDecimal salesAvgTicket, BigDecimal posAvgTicket,
                         int approvalCount, long ticketCount, BigDecimal outputVat,
                         BigDecimal inputVat, BigDecimal netVat, BigDecimal totalPurchases,
                         long lowStockCount, int expiringCount, long expiredCount, long soonCount,
                         List<TopProductsWidget.RankedProduct> topProducts,
                         List<RecentActivityWidget.ActivityEntry> recentActivities,
                         ProfitEngine.ProfitMetrics profitMetrics) {}
}
