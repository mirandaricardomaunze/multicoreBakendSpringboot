package mz.multicore.erp.modules.portal.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.service.ApprovalRequestLookup;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.service.TillSessionLookup;
import mz.multicore.erp.modules.portal.dto.PortalOpenTillDTO;
import mz.multicore.erp.modules.portal.dto.PortalOverdueClientDTO;
import mz.multicore.erp.modules.portal.dto.PortalStoreSalesDTO;
import mz.multicore.erp.modules.portal.dto.PortalSummaryDTO;
import mz.multicore.erp.modules.reports.dto.LowStockAlertDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Resumo só de leitura do painel do portal do gestor (PORTAL_GESTOR_SPEC §3.3). Lê os outros módulos
 * pelas portas de leitura deles e devolve só DTOs.
 */
@Service
public class PortalSummaryService {

    static final String NO_STORE = "Sem loja";
    static final int LOW_STOCK_LIMIT = 20;
    static final int OVERDUE_CLIENTS_LIMIT = 10;

    private final InvoiceLookup invoiceLookup;
    private final StockLookup stockLookup;
    private final TillSessionLookup tillSessionLookup;
    private final ApprovalRequestLookup approvalRequestLookup;

    public PortalSummaryService(InvoiceLookup invoiceLookup, StockLookup stockLookup,
                                TillSessionLookup tillSessionLookup, ApprovalRequestLookup approvalRequestLookup) {
        this.invoiceLookup = invoiceLookup;
        this.stockLookup = stockLookup;
        this.tillSessionLookup = tillSessionLookup;
        this.approvalRequestLookup = approvalRequestLookup;
    }

    @Transactional(readOnly = true)
    public PortalSummaryDTO buildSummary(Long companyId) {
        return buildSummary(companyId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    PortalSummaryDTO buildSummary(Long companyId, LocalDate today) {
        CurrentUserContext.requireCompany(companyId);
        List<Invoice> monthSales = invoiceLookup.listCreatedBetween(companyId,
                        today.withDayOfMonth(1).atStartOfDay(), today.plusDays(1).atStartOfDay())
                .stream().filter(Invoice::countsAsSale).toList();
        List<Invoice> todaySales = monthSales.stream().filter(invoice -> today.equals(invoice.issueDate())).toList();
        List<Invoice> overdue = invoiceLookup.listByStatus(companyId, InvoiceStatus.collectableStatuses()).stream()
                .filter(invoice -> invoice.daysOverdue(today) > 0).toList();
        return new PortalSummaryDTO(today, sum(todaySales, Invoice::payableTotal),
                sum(monthSales, Invoice::payableTotal), salesByStore(todaySales, monthSales),
                openTills(companyId), lowStock(companyId), sum(overdue, Invoice::outstandingAmount),
                overdueClients(overdue, today),
                approvalRequestLookup.listForCompanyByStatus(companyId, ApprovalStatus.PENDING).size());
    }

    private List<PortalStoreSalesDTO> salesByStore(List<Invoice> todaySales, List<Invoice> monthSales) {
        Map<String, BigDecimal> today = totalsByStore(todaySales);
        return totalsByStore(monthSales).entrySet().stream()
                .map(entry -> new PortalStoreSalesDTO(entry.getKey(),
                        today.getOrDefault(entry.getKey(), BigDecimal.ZERO), entry.getValue()))
                .sorted(Comparator.comparing(PortalStoreSalesDTO::month).reversed()
                        .thenComparing(PortalStoreSalesDTO::store))
                .toList();
    }

    private Map<String, BigDecimal> totalsByStore(List<Invoice> sales) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        sales.forEach(invoice -> totals.merge(storeOf(invoice), invoice.payableTotal(), BigDecimal::add));
        return totals;
    }

    private String storeOf(Invoice invoice) {
        return invoice.getWarehouse() == null || invoice.getWarehouse().getName() == null
                ? NO_STORE : invoice.getWarehouse().getName();
    }

    private List<PortalOpenTillDTO> openTills(Long companyId) {
        return tillSessionLookup.listForCompany(companyId).stream()
                .filter(session -> "OPEN".equals(session.getStatus()))
                .sorted(Comparator.comparing(TillSession::getOpenDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(session -> new PortalOpenTillDTO(session.getId(),
                        session.getCurrentOperator() == null ? session.getOperator() : session.getCurrentOperator(),
                        session.getOpenDate()))
                .toList();
    }

    private List<LowStockAlertDTO> lowStock(Long companyId) {
        return stockLookup.listForCompany(companyId).stream()
                .filter(this::isBelowMinimum)
                .sorted(Comparator.comparing(this::coverage)
                        .thenComparing(this::shortfall, Comparator.reverseOrder()))
                .limit(LOW_STOCK_LIMIT)
                .map(stock -> new LowStockAlertDTO(stock.getProduct().getId(), stock.getProduct().getSku(),
                        stock.getProduct().getName(), stock.getWarehouse().getName(),
                        stock.getQuantity(), stock.getProduct().getMinStock()))
                .toList();
    }

    private boolean isBelowMinimum(Stock stock) {
        BigDecimal minimum = stock.getProduct().getMinStock();
        return minimum != null && minimum.signum() > 0 && stock.getQuantity() != null
                && stock.getQuantity().compareTo(minimum) < 0;
    }

    private BigDecimal coverage(Stock stock) {
        return stock.getQuantity().divide(stock.getProduct().getMinStock(), MathContext.DECIMAL64);
    }

    private BigDecimal shortfall(Stock stock) {
        return stock.getProduct().getMinStock().subtract(stock.getQuantity());
    }

    private List<PortalOverdueClientDTO> overdueClients(List<Invoice> overdue, LocalDate today) {
        Map<String, List<Invoice>> byClient = new LinkedHashMap<>();
        overdue.forEach(invoice -> byClient.computeIfAbsent(clientKey(invoice), key -> new ArrayList<>()).add(invoice));
        return byClient.values().stream()
                .map(invoices -> toOverdueClient(invoices, today))
                .sorted(Comparator.comparing(PortalOverdueClientDTO::amount).reversed()
                        .thenComparing(PortalOverdueClientDTO::clientName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(OVERDUE_CLIENTS_LIMIT)
                .toList();
    }

    private PortalOverdueClientDTO toOverdueClient(List<Invoice> invoices, LocalDate today) {
        Invoice first = invoices.get(0);
        Long clientId = first.getClient() == null ? null : first.getClient().getId();
        int maxDays = invoices.stream().mapToInt(invoice -> invoice.daysOverdue(today)).max().orElse(0);
        return new PortalOverdueClientDTO(clientId, clientName(first),
                sum(invoices, Invoice::outstandingAmount), invoices.size(), maxDays);
    }

    private String clientKey(Invoice invoice) {
        return invoice.getClient() == null ? "name:" + clientName(invoice) : "id:" + invoice.getClient().getId();
    }

    private String clientName(Invoice invoice) {
        return invoice.getClient() == null ? invoice.getCustomerName() : invoice.getClient().getName();
    }

    private static BigDecimal sum(List<Invoice> invoices, Function<Invoice, BigDecimal> value) {
        return invoices.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
