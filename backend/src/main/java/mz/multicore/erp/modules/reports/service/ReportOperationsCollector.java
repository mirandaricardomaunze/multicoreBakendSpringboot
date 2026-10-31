package mz.multicore.erp.modules.reports.service;

import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import mz.multicore.erp.modules.reports.dto.CashMovementSummaryDTO;
import mz.multicore.erp.modules.reports.dto.LowStockAlertDTO;
import mz.multicore.erp.modules.reports.dto.OpenTillSessionDTO;
import mz.multicore.erp.modules.reports.dto.PaymentMethodSummaryDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Recolhe e normaliza indicadores operacionais externos às facturas. */
@Component
public class ReportOperationsCollector {
    private final StockRepository stockRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final TillSessionRepository tillSessionRepository;
    private final PaymentEntryRepository paymentEntryRepository;
    private final TillMovementRepository tillMovementRepository;

    public ReportOperationsCollector(StockRepository stockRepository,
                                     ApprovalRequestRepository approvalRequestRepository,
                                     TillSessionRepository tillSessionRepository,
                                     PaymentEntryRepository paymentEntryRepository,
                                     TillMovementRepository tillMovementRepository) {
        this.stockRepository = stockRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.tillSessionRepository = tillSessionRepository;
        this.paymentEntryRepository = paymentEntryRepository;
        this.tillMovementRepository = tillMovementRepository;
    }

    List<LowStockAlertDTO> lowStockAlerts(Long companyId) {
        return stockRepository.findByWarehouseCompanyId(companyId).stream()
                .filter(this::isBelowMinimum).map(this::toLowStockAlert).toList();
    }

    long countPendingApprovals(Long companyId) {
        return approvalRequestRepository.findByCompanyIdAndStatus(companyId, ApprovalStatus.PENDING).size();
    }

    List<OpenTillSessionDTO> openTillSessions(Long companyId) {
        return tillSessionRepository.findByCompanyId(companyId).stream()
                .filter(session -> "OPEN".equals(session.getStatus()))
                .map(this::toOpenTillSession).toList();
    }

    List<PaymentMethodSummaryDTO> paymentSummary(Long companyId, LocalDateTime from, LocalDateTime to) {
        return paymentEntryRepository.findByInvoiceCompanyIdAndPaidAtBetween(companyId, from, to).stream()
                .collect(Collectors.groupingBy(entry -> entry.getMethod() == null ? "UNKNOWN" : entry.getMethod().name(),
                        Collectors.reducing(BigDecimal.ZERO,
                                entry -> valueOrZero(entry.getAmount()), BigDecimal::add)))
                .entrySet().stream().map(entry -> new PaymentMethodSummaryDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PaymentMethodSummaryDTO::method)).toList();
    }

    List<CashMovementSummaryDTO> cashMovementSummary(Long companyId, LocalDateTime from, LocalDateTime to) {
        return tillMovementRepository.findByTillSessionCompanyIdAndMovementDateBetween(companyId, from, to).stream()
                .collect(Collectors.groupingBy(movement -> movement.getMovementType() == null
                                ? "UNKNOWN" : movement.getMovementType().name(),
                        Collectors.reducing(BigDecimal.ZERO,
                                movement -> valueOrZero(movement.getAmount()), BigDecimal::add)))
                .entrySet().stream().map(entry -> new CashMovementSummaryDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CashMovementSummaryDTO::movementType)).toList();
    }

    private boolean isBelowMinimum(Stock stock) {
        BigDecimal minimum = stock.getProduct().getMinStock();
        return minimum != null && stock.getQuantity().compareTo(minimum) < 0;
    }

    private LowStockAlertDTO toLowStockAlert(Stock stock) {
        return new LowStockAlertDTO(stock.getProduct().getId(), stock.getProduct().getSku(),
                stock.getProduct().getName(), stock.getWarehouse().getName(),
                stock.getQuantity(), stock.getProduct().getMinStock());
    }

    private OpenTillSessionDTO toOpenTillSession(TillSession session) {
        return new OpenTillSessionDTO(session.getId(), session.getOperator(),
                session.getOpeningBalance(), session.getOpenDate());
    }

    private BigDecimal valueOrZero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
