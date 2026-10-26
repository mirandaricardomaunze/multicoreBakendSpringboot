package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Orquestrador executivo para registo, aprovação e monitorização analítica de quebras de stock.
 * Delega o ciclo de vida para colaboradores especializados respeitando os limites físicos de linhas.
 */
@Service
public class StockWasteService {

    public static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("2500.00");

    private final StockWasteLifecycleService lifecycleService;
    private final StockWasteAnalyticsService analyticsService;
    private final StockWasteQueryService queryService;

    @Autowired
    public StockWasteService(
            StockWasteLifecycleService lifecycleService,
            StockWasteAnalyticsService analyticsService,
            StockWasteQueryService queryService
    ) {
        this.lifecycleService = lifecycleService;
        this.analyticsService = analyticsService;
        this.queryService = queryService;
    }

    public StockWasteService(
            StockWasteRepository wasteRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            ProductBatchRepository batchRepository,
            InventoryService inventoryService,
            InvoiceRepository invoiceRepository,
            CompanyRepository companyRepository
    ) {
        this(wasteRepository, productRepository, warehouseRepository, batchRepository,
                inventoryService, invoiceRepository, companyRepository, null, null);
    }

    public StockWasteService(
            StockWasteRepository wasteRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            ProductBatchRepository batchRepository,
            InventoryService inventoryService,
            InvoiceRepository invoiceRepository,
            CompanyRepository companyRepository,
            AuditLogService auditLogService
    ) {
        this(wasteRepository, productRepository, warehouseRepository, batchRepository,
                inventoryService, invoiceRepository, companyRepository, auditLogService, null);
    }

    public StockWasteService(
            StockWasteRepository wasteRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            ProductBatchRepository batchRepository,
            InventoryService inventoryService,
            InvoiceRepository invoiceRepository,
            CompanyRepository companyRepository,
            AuditLogService auditLogService,
            @Lazy ApprovalService approvalService
    ) {
        this.lifecycleService = new StockWasteLifecycleService(
                wasteRepository, productRepository, warehouseRepository, batchRepository,
                inventoryService, companyRepository, auditLogService, approvalService);
        this.analyticsService = new StockWasteAnalyticsService(
                batchRepository, wasteRepository, invoiceRepository);
        this.queryService = new StockWasteQueryService(wasteRepository);
    }

    @Transactional
    public StockWasteDTO registerWaste(CreateStockWasteRequest req) {
        return lifecycleService.registerWaste(req);
    }

    @Transactional
    public StockWasteDTO approveWaste(Long id, ApproveWasteRequest req) {
        return lifecycleService.approveWaste(id, req);
    }

    @Transactional(readOnly = true)
    public List<StockWasteDTO> findByCompany(Long companyId) {
        return queryService.findByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<StockWasteDTO> findByCompanyAndStatus(Long companyId, WasteStatus status) {
        return queryService.findByCompanyAndStatus(companyId, status);
    }

    @Transactional(readOnly = true)
    public List<ExpiringBatchAlertDTO> getExpiringBatchesRadar(Long companyId, int days) {
        return analyticsService.getExpiringBatchesRadar(companyId, days);
    }

    @Transactional(readOnly = true)
    public WasteSummaryDTO getSummary(Long companyId, LocalDate start, LocalDate end) {
        return analyticsService.getSummary(companyId, start, end);
    }

    public StockWasteDTO toDTO(StockWaste w) {
        return StockWasteQueryService.toDTO(w);
    }
}
