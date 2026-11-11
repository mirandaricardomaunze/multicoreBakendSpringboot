package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.dto.CreateStockAdjustmentRequest;
import mz.multicore.erp.modules.inventory.dto.RegisterMovementRequest;
import mz.multicore.erp.modules.inventory.dto.StockMovementDTO;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Orquestrador transacional de movimentos e ajustes de stock com controlo de concorrência pessimista,
 * idempotência SHA-256 e rastreabilidade FEFO.
 */
@Service
public class StockMovementService {

    private final StockMovementRegistrationService registrationService;
    private final StockAdjustmentCoordinator adjustmentCoordinator;
    private final StockMovementQueryService queryService;

    @Autowired
    public StockMovementService(
            StockMovementRegistrationService registrationService,
            StockAdjustmentCoordinator adjustmentCoordinator,
            StockMovementQueryService queryService
    ) {
        this.registrationService = registrationService;
        this.adjustmentCoordinator = adjustmentCoordinator;
        this.queryService = queryService;
    }

    public StockMovementService(
            StockMovementRepository stockMovementRepository,
            StockMovementOperationRepository stockMovementOperationRepository,
            StockAdjustmentOperationRepository stockAdjustmentOperationRepository,
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            CompanyLookup companyLookup,
            ProductBatchService productBatchService,
            AuditLogService auditLogService
    ) {
        StockMovementRegistrationService reg = new StockMovementRegistrationService(
                stockMovementRepository, stockMovementOperationRepository, stockRepository,
                warehouseRepository, productRepository, companyLookup, productBatchService, null);
        StockMovementQueryService query = new StockMovementQueryService(stockMovementRepository);
        StockAdjustmentCoordinator adj = new StockAdjustmentCoordinator(
                stockAdjustmentOperationRepository, stockRepository, warehouseRepository,
                productRepository, companyLookup, auditLogService, reg, query);
        this.registrationService = reg;
        this.adjustmentCoordinator = adj;
        this.queryService = query;
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getMovementsByCompany(Long companyId) {
        return queryService.getMovementsByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> findMovementsByCompany(Long companyId) {
        return queryService.findMovementsByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementDTO> findMovementsPageByCompany(
            Long companyId, Integer page, Integer size) {
        return queryService.findMovementsPageByCompany(companyId, page, size);
    }

    @Transactional
    public StockMovement registerMovement(
            Product product,
            Warehouse warehouse,
            BigDecimal quantity,
            String movementType,
            String batchNumber,
            String serialNumber,
            String description
    ) {
        return registrationService.registerMovement(product, warehouse, quantity, movementType,
                batchNumber, serialNumber, description, null);
    }

    @Transactional
    public StockMovement registerMovement(
            Product product,
            Warehouse warehouse,
            BigDecimal quantity,
            String movementType,
            String batchNumber,
            String serialNumber,
            String description,
            LocalDate expirationDate
    ) {
        return registrationService.registerMovement(product, warehouse, quantity, movementType,
                batchNumber, serialNumber, description, expirationDate);
    }

    public void adjustStock(Product product, Warehouse warehouse, BigDecimal delta) {
        registrationService.adjustStock(product, warehouse, delta);
    }

    @Transactional
    public StockMovementDTO registerMovement(RegisterMovementRequest request) {
        return registrationService.registerMovementWithIdempotency(request, queryService);
    }

    @Transactional
    public StockMovementDTO adjustStock(CreateStockAdjustmentRequest request) {
        return adjustmentCoordinator.adjustStock(request);
    }

    public StockMovementDTO toDTO(StockMovement m) {
        return queryService.toDTO(m);
    }
}
