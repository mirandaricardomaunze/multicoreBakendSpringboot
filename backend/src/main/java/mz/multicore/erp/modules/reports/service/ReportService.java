package mz.multicore.erp.modules.reports.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.repository.ApprovalRequestRepository;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.inventory.service.StockLookup;
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
    private final InvoiceLookup invoiceLookup;
    private final ReportSalesAnalyzer salesAnalyzer;
    private final ReportOperationsCollector operationsCollector;

    @Autowired
    public ReportService(InvoiceLookup invoiceLookup, ReportSalesAnalyzer salesAnalyzer,
                         ReportOperationsCollector operationsCollector) {
        this.invoiceLookup = invoiceLookup;
        this.salesAnalyzer = salesAnalyzer;
        this.operationsCollector = operationsCollector;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public ReportService(InvoiceLookup invoiceLookup, StockLookup stockLookup,
                         ApprovalRequestRepository approvalRequestRepository,
                         TillSessionRepository tillSessionRepository,
                         PaymentEntryRepository paymentEntryRepository,
                         TillMovementRepository tillMovementRepository) {
        this(invoiceLookup, new ReportSalesAnalyzer(),
                new ReportOperationsCollector(stockLookup, approvalRequestRepository,
                        tillSessionRepository, paymentEntryRepository, tillMovementRepository));
    }

    @Transactional(readOnly = true)
    public StoreDashboardDTO buildStoreDashboard(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        LocalDate today = LocalDate.now();
        List<Invoice> salesToday = salesAnalyzer.filterRealisedSales(
                invoiceLookup.listCreatedBetween(
                        companyId, today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        List<Invoice> collectable = invoiceLookup.listByStatus(
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
                invoiceLookup.listCreatedBetween(companyId, from, to));
        return new DailyStoreReportDTO(reportDate, salesAnalyzer.summarise(sales),
                salesAnalyzer.outstandingCreditFor(sales), operationsCollector.paymentSummary(companyId, from, to),
                operationsCollector.cashMovementSummary(companyId, from, to),
                salesAnalyzer.topProductsFrom(sales), salesAnalyzer.salesByOperator(sales),
                salesAnalyzer.marginByProduct(sales));
    }
}
