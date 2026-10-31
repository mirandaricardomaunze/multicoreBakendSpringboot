package mz.multicore.erp.modules.reports.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import mz.multicore.erp.modules.reports.dto.DailyStoreReportDTO;
import mz.multicore.erp.modules.reports.dto.StoreDashboardDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Orquestra os relatórios operacionais tenant-scoped da loja. */
@Service
public class ReportService {
    private final InvoiceRepository invoiceRepository;
    private final ReportSalesAnalyzer salesAnalyzer;
    private final ReportOperationsCollector operationsCollector;

    @Autowired
    public ReportService(InvoiceRepository invoiceRepository, ReportSalesAnalyzer salesAnalyzer,
                         ReportOperationsCollector operationsCollector) {
        this.invoiceRepository = invoiceRepository;
        this.salesAnalyzer = salesAnalyzer;
        this.operationsCollector = operationsCollector;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public ReportService(InvoiceRepository invoiceRepository, StockRepository stockRepository,
                         ApprovalRequestRepository approvalRequestRepository,
                         TillSessionRepository tillSessionRepository,
                         PaymentEntryRepository paymentEntryRepository,
                         TillMovementRepository tillMovementRepository) {
        this(invoiceRepository, new ReportSalesAnalyzer(),
                new ReportOperationsCollector(stockRepository, approvalRequestRepository,
                        tillSessionRepository, paymentEntryRepository, tillMovementRepository));
    }

    @Transactional(readOnly = true)
    public StoreDashboardDTO buildStoreDashboard(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        LocalDate today = LocalDate.now();
        List<Invoice> salesToday = salesAnalyzer.filterRealisedSales(
                invoiceRepository.findByCompanyIdAndCreatedAtBetween(
                        companyId, today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        List<Invoice> collectable = invoiceRepository.findByCompanyIdAndStatusIn(
                companyId, InvoiceStatus.collectableStatuses());
        return new StoreDashboardDTO(today, salesAnalyzer.summarise(salesToday),
                salesAnalyzer.topProductsFrom(salesToday), operationsCollector.lowStockAlerts(companyId),
                operationsCollector.countPendingApprovals(companyId),
                salesAnalyzer.unpaidInvoicesTotal(collectable), operationsCollector.openTillSessions(companyId));
    }

    @Transactional(readOnly = true)
    public DailyStoreReportDTO buildDailyStoreReport(Long companyId, LocalDate date) {
        CurrentUserContext.requireCompany(companyId);
        LocalDate reportDate = date == null ? LocalDate.now() : date;
        LocalDateTime from = reportDate.atStartOfDay();
        LocalDateTime to = reportDate.plusDays(1).atStartOfDay();
        List<Invoice> sales = salesAnalyzer.filterRealisedSales(
                invoiceRepository.findByCompanyIdAndCreatedAtBetween(companyId, from, to));
        return new DailyStoreReportDTO(reportDate, salesAnalyzer.summarise(sales),
                salesAnalyzer.outstandingCreditFor(sales), operationsCollector.paymentSummary(companyId, from, to),
                operationsCollector.cashMovementSummary(companyId, from, to),
                salesAnalyzer.topProductsFrom(sales), salesAnalyzer.salesByOperator(sales),
                salesAnalyzer.marginByProduct(sales));
    }
}
