package com.phcpro.modules.reports.service;

import com.phcpro.modules.approvals.model.ApprovalStatus;
import com.phcpro.modules.approvals.repository.ApprovalRequestRepository;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceLine;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.inventory.model.Stock;
import com.phcpro.modules.inventory.repository.StockRepository;
import com.phcpro.modules.pos.model.TillSession;
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

    public ReportService(
            InvoiceRepository invoiceRepository,
            StockRepository stockRepository,
            ApprovalRequestRepository approvalRequestRepository,
            TillSessionRepository tillSessionRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.stockRepository = stockRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.tillSessionRepository = tillSessionRepository;
    }

    @Transactional(readOnly = true)
    public StoreDashboardDTO buildStoreDashboard(Long companyId) {
        LocalDate today = LocalDate.now();
        List<Invoice> invoices = invoiceRepository.findByCompanyId(companyId);
        List<Invoice> salesToday = filterSalesToday(invoices, today);

        return new StoreDashboardDTO(
                today,
                summarise(salesToday),
                topProductsFrom(salesToday),
                lowStockAlertsForCompany(companyId),
                countPendingApprovals(),
                unpaidInvoicesTotal(invoices),
                openTillSessionsForCompany(companyId)
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
        Map<Long, long[]> qtyByProduct = new java.util.HashMap<>();
        Map<Long, BigDecimal> revenueByProduct = new java.util.HashMap<>();
        Map<Long, String[]> namesByProduct = new java.util.HashMap<>();

        for (Invoice inv : sales) {
            for (InvoiceLine line : inv.getLines()) {
                Long productId = line.getProduct().getId();
                qtyByProduct.computeIfAbsent(productId, k -> new long[]{0})[0] += line.getQuantity();
                revenueByProduct.merge(productId, line.getLineTotal() != null ? line.getLineTotal() : BigDecimal.ZERO, BigDecimal::add);
                namesByProduct.putIfAbsent(productId, new String[]{line.getProduct().getSku(), line.getProduct().getName()});
            }
        }

        return qtyByProduct.entrySet().stream()
                .map(e -> new TopProductDTO(
                        e.getKey(),
                        namesByProduct.get(e.getKey())[0],
                        namesByProduct.get(e.getKey())[1],
                        e.getValue()[0],
                        revenueByProduct.getOrDefault(e.getKey(), BigDecimal.ZERO)
                ))
                .sorted(Comparator.comparingLong(TopProductDTO::quantitySold).reversed())
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

    private long countPendingApprovals() {
        return approvalRequestRepository.findByStatus(ApprovalStatus.PENDING).size();
    }

    private BigDecimal unpaidInvoicesTotal(List<Invoice> invoices) {
        return invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.APPROVED)
                .map(Invoice::getTotalAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
