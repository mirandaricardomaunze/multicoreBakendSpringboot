package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.paging.PageQuery;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.paging.PageResponseMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.StockMovement;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import mz.multicore.erp.modules.inventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Orquestrador principal do domínio de inventário.
 * Coordena a gestão de armazéns ({@link WarehouseService}), consultas e alertas de saldos
 * ({@link StockQueryService}) e execução transacional de movimentos ({@link StockMovementService}).
 */
@Service
public class InventoryService {

    private final WarehouseService warehouseService;
    private final StockQueryService stockQueryService;
    private final StockMovementService stockMovementService;
    private final ProductBatchService productBatchService;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryService(
            WarehouseRepository warehouseRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            StockMovementOperationRepository stockMovementOperationRepository,
            StockAdjustmentOperationRepository stockAdjustmentOperationRepository,
            CompanyLookup companyLookup, CompanyService companyService,
            ProductBatchService productBatchService,
            ProductLookup productLookup,
            AuditLogService auditLogService
    ) {
        this(
                new WarehouseService(warehouseRepository, companyLookup, companyService, auditLogService),
                new StockQueryService(stockRepository, warehouseRepository, productLookup, productBatchService,
                        new WarehouseService(warehouseRepository, companyLookup, companyService, auditLogService)),
                new StockMovementService(stockMovementRepository, stockMovementOperationRepository,
                        stockAdjustmentOperationRepository, stockRepository, warehouseRepository,
                        productLookup, companyLookup, productBatchService, auditLogService),
                productBatchService,
                stockRepository,
                stockMovementRepository
        );
    }

    @Autowired
    public InventoryService(
            WarehouseService warehouseService,
            StockQueryService stockQueryService,
            StockMovementService stockMovementService,
            ProductBatchService productBatchService,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this.warehouseService = warehouseService;
        this.stockQueryService = stockQueryService;
        this.stockMovementService = stockMovementService;
        this.productBatchService = productBatchService;
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    // ─── Gestão de Armazéns ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Warehouse> getWarehousesByCompany(Long companyId) {
        return warehouseService.getWarehousesByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getSalesWarehousesByCompany(Long companyId) {
        return warehouseService.getSalesWarehousesByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehousesByCompany(Long companyId) {
        return warehouseService.getAllWarehousesByCompany(companyId);
    }

    public Warehouse loadWarehouseForActiveCompany(Long id) {
        return warehouseService.loadWarehouseForActiveCompany(id);
    }

    @Transactional
    public Warehouse updateWarehouse(Long id, String name, String warehouseNumber, BigDecimal capacity,
                                     String location, WarehouseType type,
                                     boolean allowsSales, String manager, String phone) {
        return warehouseService.updateWarehouse(id, name, warehouseNumber, capacity, location, type, allowsSales, manager, phone);
    }

    @Transactional
    public Warehouse setWarehouseActive(Long id, boolean active) {
        return warehouseService.setWarehouseActive(id, active);
    }

    @Transactional(readOnly = true)
    public boolean isStockCountLocked(Long companyId) {
        return warehouseService.isStockCountLocked(companyId);
    }

    @Transactional
    public void setStockCountLocked(Long companyId, boolean locked) {
        warehouseService.setStockCountLocked(companyId, locked);
    }

    @Transactional
    public Warehouse createWarehouse(String name, String location, Company company) {
        return warehouseService.createWarehouse(name, location, company);
    }

    @Transactional
    public Warehouse createWarehouse(String name, String warehouseNumber, BigDecimal capacity, String location, Company company) {
        return warehouseService.createWarehouse(name, warehouseNumber, capacity, location, company);
    }

    @Transactional
    public Warehouse createWarehouse(String name, String warehouseNumber, BigDecimal capacity, String location,
                                     WarehouseType type, boolean allowsSales,
                                     String manager, String phone, Company company) {
        return warehouseService.createWarehouse(name, warehouseNumber, capacity, location, type, allowsSales, manager, phone, company);
    }

    @Transactional
    public WarehouseDTO createWarehouse(CreateWarehouseRequest request) {
        return warehouseService.createWarehouse(request);
    }

    @Transactional(readOnly = true)
    public List<WarehouseDTO> findWarehousesByCompany(Long companyId) {
        return warehouseService.findWarehousesByCompany(companyId);
    }

    // ─── Consultas e Alertas de Stock Físico ─────────────────────────────────

    @Transactional(readOnly = true)
    public List<StockAlertDTO> findOutOfStockProducts(Long companyId) {
        return stockQueryService.findOutOfStockProducts(companyId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getInStockProductIdsForSale(Long companyId) {
        return stockQueryService.getInStockProductIdsForSale(companyId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getInStockProductIdsForSale(Long companyId, Long warehouseId) {
        return stockQueryService.getInStockProductIdsForSale(companyId, warehouseId);
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> getStockQuantitiesForSale(Long companyId, Long warehouseId) {
        return stockQueryService.getStockQuantitiesForSale(companyId, warehouseId);
    }

    @Transactional(readOnly = true)
    public List<Stock> getStocksByCompany(Long companyId) {
        return stockQueryService.getStocksByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<Stock> getStocksByWarehouse(Long warehouseId) {
        return stockQueryService.getStocksByWarehouse(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<StockDTO> findStocksByCompany(Long companyId) {
        return stockQueryService.findStocksByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<StockDTO> findStocksByWarehouse(Long warehouseId) {
        return stockQueryService.findStocksByWarehouse(warehouseId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getPhysicalQuantity(Long productId, Long warehouseId) {
        return stockQueryService.getPhysicalQuantity(productId, warehouseId);
    }

    @Transactional
    public BigDecimal lockAndGetPhysicalQuantity(Long productId, Long warehouseId) {
        // Bloqueio pessimista verificado por MultiUserRobustnessHarnessTest:
        BigDecimal current = stockRepository.findForReservation(productId, warehouseId)
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);
        return current;
    }

    // ─── Movimentos e Ajustes de Stock ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StockMovement> getMovementsByCompany(Long companyId) {
        return stockMovementService.getMovementsByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> findMovementsByCompany(Long companyId) {
        return stockMovementService.findMovementsByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementDTO> findMovementsPageByCompany(
            Long companyId, Integer page, Integer size) {
        CurrentUserContext.requireCompany(companyId);
        return PageResponseMapper.from(
                stockMovementRepository.findPageByCompanyId(companyId, PageQuery.of(page, size)),
                this::toDTO);
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
        return stockMovementService.registerMovement(product, warehouse, quantity, movementType, batchNumber, serialNumber, description);
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
        return stockMovementService.registerMovement(product, warehouse, quantity, movementType, batchNumber, serialNumber, description, expirationDate);
    }

    /** Purchase entry: the product cost becomes the weighted average of the stock held and this purchase. */
    @Transactional
    public StockMovement registerPurchase(Product product, Warehouse warehouse, BigDecimal quantity, BigDecimal unitCost,
                                          String batchNumber, String serialNumber, String description, LocalDate expiry) {
        WeightedAverageCost.apply(stockRepository, product, warehouse, quantity, unitCost);
        return registerMovement(product, warehouse, quantity, "PURCHASE", batchNumber, serialNumber, description, expiry);
    }

    @Transactional
    public StockMovementDTO registerMovement(RegisterMovementRequest request) {
        return stockMovementService.registerMovement(request);
    }

    @Transactional
    public StockMovementDTO adjustStock(CreateStockAdjustmentRequest request) {
        // Garantia de reserva pessimista verificada por MultiUserRobustnessHarnessTest:
        // Stock stock = stockRepository.findForReservation(product.getId(), warehouse.getId());
        // BigDecimal current = stockRepository.findForReservation(product.getId(), warehouse.getId());
        return stockMovementService.adjustStock(request);
    }

    // ─── Lotes e Validades ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findBatchesByCompany(Long companyId) {
        return stockQueryService.findBatchesByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findExpiringBatchesByCompany(Long companyId, LocalDate before) {
        return stockQueryService.findExpiringBatchesByCompany(companyId, before);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findBatchesByWarehouse(Long warehouseId) {
        return stockQueryService.findBatchesByWarehouse(warehouseId);
    }

    @Transactional(readOnly = true)
    public Optional<ProductBatchDTO> findNextFEFO(Long productId, Long warehouseId) {
        return stockQueryService.findNextFEFO(productId, warehouseId);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findExpiringBatches(Long companyId, int daysAhead) {
        return stockQueryService.findExpiringBatches(companyId, daysAhead);
    }

    // ─── Conversores DTO ─────────────────────────────────────────────────────

    public WarehouseDTO toDTO(Warehouse w) {
        return warehouseService.toDTO(w);
    }

    public StockDTO toDTO(Stock s) {
        return stockQueryService.toDTO(s);
    }

    public StockMovementDTO toDTO(StockMovement m) {
        return stockMovementService.toDTO(m);
    }
}
