package com.phcpro.modules.reports.service;

import com.phcpro.modules.approvals.model.ApprovalStatus;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.approvals.repository.ApprovalRequestRepository;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceLine;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.inventory.model.Stock;
import com.phcpro.modules.inventory.repository.StockRepository;
import com.phcpro.modules.pos.model.TillSession;
import com.phcpro.modules.pos.repository.PaymentEntryRepository;
import com.phcpro.modules.pos.repository.TillMovementRepository;
import com.phcpro.modules.pos.repository.TillSessionRepository;
import com.phcpro.modules.reports.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final int TOP_PRODUCTS_LIMIT = 5;

    private final InvoiceRepository invoiceRepository;
    private final StockRepository stockRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final TillSessionRepository tillSessionRepository;
    private final PaymentEntryRepository paymentEntryRepository;
    private final TillMovementRepository tillMovementRepository;

    public ReportService(
            InvoiceRepository invoiceRepository,
            StockRepository stockRepository,
            ApprovalRequestRepository approvalRequestRepository,
            TillSessionRepository tillSessionRepository,
            PaymentEntryRepository paymentEntryRepository,
            TillMovementRepository tillMovementRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.stockRepository = stockRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.tillSessionRepository = tillSessionRepository;
        this.paymentEntryRepository = paymentEntryRepository;
        this.tillMovementRepository = tillMovementRepository;
    }

    @Transactional(readOnly = true)
    public StoreDashboardDTO buildStoreDashboard(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        LocalDate today = LocalDate.now();
        List<Invoice> invoices = invoiceRepository.findByCompanyId(companyId);
        List<Invoice> salesToday = filterSalesToday(invoices, today);

        return new StoreDashboardDTO(
                today,
                summarise(salesToday),
                topProductsFrom(salesToday),
                lowStockAlertsForCompany(companyId),
                countPendingApprovals(companyId),
                unpaidInvoicesTotal(invoices),
                openTillSessionsForCompany(companyId)
        );
    }

    @Transactional(readOnly = true)
    public DailyStoreReportDTO buildDailyStoreReport(Long companyId, LocalDate date) {
        CurrentUserContext.requireCompany(companyId);
        LocalDate reportDate = date == null ? LocalDate.now() : date;
        LocalDateTime from = reportDate.atStartOfDay();
        LocalDateTime to = reportDate.plusDays(1).atStartOfDay();
        List<Invoice> invoices = invoiceRepository.findByCompanyId(companyId);
        List<Invoice> sales = invoices.stream()
                .filter(inv -> inv.getCreatedAt() != null)
                .filter(inv -> !inv.getCreatedAt().isBefore(from) && inv.getCreatedAt().isBefore(to))
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED && inv.getStatus() != InvoiceStatus.REJECTED)
                .toList();

        return new DailyStoreReportDTO(
                reportDate,
                summarise(sales),
                outstandingCreditFor(sales),
                paymentSummary(companyId, from, to),
                cashMovementSummary(companyId, from, to),
                topProductsFrom(sales),
                salesByOperator(sales),
                marginByProduct(sales)
        );
    }

    private List<Invoice> filterSalesToday(List<Invoice> invoices, LocalDate today) {
        return invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .filter(inv -> inv.getCreatedAt() != null && inv.getCreatedAt().toLocalDate().equals(today))
                .toList();
    }

    private SalesSummaryDTO summarise(List<Invoice> sales) {
        BigDecimal total = sales.stream()
                .map(Invoice::getTotalAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SalesSummaryDTO(sales.size(), total);
    }

    private List<TopProductDTO> topProductsFrom(List<Invoice> sales) {
        Map<Long, BigDecimal> qtyByProduct = new java.util.HashMap<>();
        Map<Long, BigDecimal> revenueByProduct = new java.util.HashMap<>();
        Map<Long, String[]> namesByProduct = new java.util.HashMap<>();

        for (Invoice inv : sales) {
            for (InvoiceLine line : inv.getLines()) {
                Long productId = line.getProduct().getId();
                qtyByProduct.merge(productId,
                        line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO,
                        BigDecimal::add);
                revenueByProduct.merge(productId, line.getLineTotal() != null ? line.getLineTotal() : BigDecimal.ZERO, BigDecimal::add);
                namesByProduct.putIfAbsent(productId, new String[]{line.getProduct().getSku(), line.getProduct().getName()});
            }
        }

        return qtyByProduct.entrySet().stream()
                .map(e -> new TopProductDTO(
                        e.getKey(),
                        namesByProduct.get(e.getKey())[0],
                        namesByProduct.get(e.getKey())[1],
                        e.getValue(),
                        revenueByProduct.getOrDefault(e.getKey(), BigDecimal.ZERO)
                ))
                .sorted(Comparator.comparing(TopProductDTO::quantitySold).reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();
    }

    private List<LowStockAlertDTO> lowStockAlertsForCompany(Long companyId) {
        return stockRepository.findByWarehouseCompanyId(companyId).stream()
                .filter(this::isBelowMinimum)
                .map(this::toLowStockAlert)
                .collect(Collectors.toList());
    }

    private boolean isBelowMinimum(Stock stock) {
        BigDecimal min = stock.getProduct().getMinStock();
        return min != null && stock.getQuantity().compareTo(min) < 0;
    }

    private LowStockAlertDTO toLowStockAlert(Stock stock) {
        return new LowStockAlertDTO(
                stock.getProduct().getId(),
                stock.getProduct().getSku(),
                stock.getProduct().getName(),
                stock.getWarehouse().getName(),
                stock.getQuantity(),
                stock.getProduct().getMinStock()
        );
    }

    private long countPendingApprovals(Long companyId) {
        return approvalRequestRepository.findByCompanyIdAndStatus(companyId, ApprovalStatus.PENDING).size();
    }

    private BigDecimal unpaidInvoicesTotal(List<Invoice> invoices) {
        return invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.APPROVED)
                .map(Invoice::getTotalAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal outstandingCreditFor(List<Invoice> invoices) {
        return invoices.stream()
                .map(inv -> {
                    BigDecimal total = inv.getTotalAmount() == null ? BigDecimal.ZERO : inv.getTotalAmount();
                    BigDecimal paid = inv.getAmountPaid() == null ? BigDecimal.ZERO : inv.getAmountPaid();
                    BigDecimal remaining = total.subtract(paid);
                    return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<PaymentMethodSummaryDTO> paymentSummary(Long companyId, LocalDateTime from, LocalDateTime to) {
        return paymentEntryRepository.findByInvoiceCompanyIdAndPaidAtBetween(companyId, from, to).stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getMethod() == null ? "UNKNOWN" : entry.getMethod().name(),
                        Collectors.reducing(BigDecimal.ZERO,
                                entry -> entry.getAmount() == null ? BigDecimal.ZERO : entry.getAmount(),
                                BigDecimal::add)))
                .entrySet().stream()
                .map(e -> new PaymentMethodSummaryDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PaymentMethodSummaryDTO::method))
                .toList();
    }

    private List<CashMovementSummaryDTO> cashMovementSummary(Long companyId, LocalDateTime from, LocalDateTime to) {
        return tillMovementRepository.findByTillSessionCompanyIdAndMovementDateBetween(companyId, from, to).stream()
                .collect(Collectors.groupingBy(
                        movement -> movement.getMovementType() == null ? "UNKNOWN" : movement.getMovementType().name(),
                        Collectors.reducing(BigDecimal.ZERO,
                                movement -> movement.getAmount() == null ? BigDecimal.ZERO : movement.getAmount(),
                                BigDecimal::add)))
                .entrySet().stream()
                .map(e -> new CashMovementSummaryDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CashMovementSummaryDTO::movementType))
                .toList();
    }

    private List<OperatorSalesSummaryDTO> salesByOperator(List<Invoice> sales) {
        Map<String, List<Invoice>> byOperator = sales.stream()
                .collect(Collectors.groupingBy(inv ->
                        inv.getCreatedBy() == null || inv.getCreatedBy().isBlank() ? "SYSTEM" : inv.getCreatedBy()));
        return byOperator.entrySet().stream()
                .map(e -> new OperatorSalesSummaryDTO(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream()
                                .map(inv -> inv.getTotalAmount() == null ? BigDecimal.ZERO : inv.getTotalAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(OperatorSalesSummaryDTO::total).reversed())
                .toList();
    }

    private List<ProductMarginDTO> marginByProduct(List<Invoice> sales) {
        Map<Long, ProductMarginAccumulator> acc = new java.util.HashMap<>();
        for (Invoice invoice : sales) {
            for (InvoiceLine line : invoice.getLines()) {
                Long productId = line.getProduct().getId();
                ProductMarginAccumulator current = acc.computeIfAbsent(productId, id -> new ProductMarginAccumulator(
                        line.getProduct().getSku(),
                        line.getProduct().getName()));
                BigDecimal revenue = line.getLineTotal() == null ? BigDecimal.ZERO : line.getLineTotal();
                BigDecimal purchasePrice = line.getProduct().getPurchasePrice() == null
                        ? BigDecimal.ZERO : line.getProduct().getPurchasePrice();
                BigDecimal quantity = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
                current.revenue = current.revenue.add(revenue);
                current.estimatedCost = current.estimatedCost.add(purchasePrice.multiply(quantity));
            }
        }
        return acc.entrySet().stream()
                .map(e -> new ProductMarginDTO(
                        e.getKey(),
                        e.getValue().sku,
                        e.getValue().name,
                        e.getValue().revenue,
                        e.getValue().estimatedCost,
                        e.getValue().revenue.subtract(e.getValue().estimatedCost)))
                .sorted(Comparator.comparing(ProductMarginDTO::grossMargin).reversed())
                .toList();
    }

    private static class ProductMarginAccumulator {
        private final String sku;
        private final String name;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal estimatedCost = BigDecimal.ZERO;

        private ProductMarginAccumulator(String sku, String name) {
            this.sku = sku;
            this.name = name;
        }
    }

    private List<OpenTillSessionDTO> openTillSessionsForCompany(Long companyId) {
        return tillSessionRepository.findByCompanyId(companyId).stream()
                .filter(s -> "OPEN".equals(s.getStatus()))
                .map(this::toOpenTillSession)
                .toList();
    }

    private OpenTillSessionDTO toOpenTillSession(TillSession s) {
        LocalDateTime opened = s.getOpenDate();
        return new OpenTillSessionDTO(s.getId(), s.getOperator(), s.getOpeningBalance(), opened);
    }
}
