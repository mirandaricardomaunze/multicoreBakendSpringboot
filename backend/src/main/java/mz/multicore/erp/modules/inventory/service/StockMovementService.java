package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageQuery;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.paging.PageResponseMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.dto.CreateStockAdjustmentRequest;
import mz.multicore.erp.modules.inventory.dto.RegisterMovementRequest;
import mz.multicore.erp.modules.inventory.dto.StockMovementDTO;
import mz.multicore.erp.modules.inventory.model.*;
import mz.multicore.erp.modules.inventory.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor transacional de movimentos e ajustes de stock com controlo de concorrência pessimista,
 * idempotência SHA-256 e rastreabilidade FEFO.
 */
@Service
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final StockMovementOperationRepository stockMovementOperationRepository;
    private final StockAdjustmentOperationRepository stockAdjustmentOperationRepository;
    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final ProductBatchService productBatchService;
    private final AuditLogService auditLogService;

    public StockMovementService(
            StockMovementRepository stockMovementRepository,
            StockMovementOperationRepository stockMovementOperationRepository,
            StockAdjustmentOperationRepository stockAdjustmentOperationRepository,
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            CompanyRepository companyRepository,
            ProductBatchService productBatchService,
            AuditLogService auditLogService
    ) {
        this.stockMovementRepository = stockMovementRepository;
        this.stockMovementOperationRepository = stockMovementOperationRepository;
        this.stockAdjustmentOperationRepository = stockAdjustmentOperationRepository;
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.productBatchService = productBatchService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getMovementsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return stockMovementRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> findMovementsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return stockMovementRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
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
        return registerMovement(product, warehouse, quantity, movementType,
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
        if (product != null && !product.isStockTracked()) {
            return null;
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("Quantidade do movimento não pode ser zero.");
        }

        StockMovementType type;
        try {
            type = StockMovementType.valueOf(movementType == null ? "" : movementType.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Tipo de movimento de stock inválido: " + movementType);
        }

        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            return registerEntry(product, warehouse, quantity, type,
                    batchNumber, serialNumber, description, expirationDate);
        }
        return registerExit(product, warehouse, quantity.negate(), type,
                serialNumber, description);
    }

    private StockMovement registerEntry(Product product, Warehouse warehouse, BigDecimal quantity,
                                         StockMovementType movementType, String batchNumber, String serialNumber,
                                         String description, LocalDate expirationDate) {
        ProductBatch batch = productBatchService.addToBatch(
                product, warehouse, batchNumber, expirationDate, quantity);

        StockMovement movement = saveMovement(product, warehouse, quantity, movementType,
                batch.getBatchNumber(), batch, serialNumber, description);

        adjustStock(product, warehouse, quantity);
        return movement;
    }

    private StockMovement registerExit(Product product, Warehouse warehouse, BigDecimal absQty,
                                        StockMovementType movementType, String serialNumber, String description) {
        ensureLegacyBatchIfNeeded(product, warehouse);

        List<ProductBatchService.BatchConsumption> debits =
                productBatchService.consumeFEFO(product, warehouse, absQty);

        List<StockMovement> generated = new ArrayList<>(debits.size());
        for (ProductBatchService.BatchConsumption d : debits) {
            StockMovement movement = saveMovement(product, warehouse, d.quantity().negate(),
                    movementType, d.batch().getBatchNumber(), d.batch(), serialNumber, description);
            generated.add(movement);
        }

        adjustStock(product, warehouse, absQty.negate());
        return generated.get(generated.size() - 1);
    }

    private void ensureLegacyBatchIfNeeded(Product product, Warehouse warehouse) {
        productBatchService.materialiseLegacyIfNeeded(product, warehouse,
                stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                        .map(Stock::getQuantity)
                        .orElse(null));
    }

    private StockMovement saveMovement(Product product, Warehouse warehouse, BigDecimal qty,
                                        StockMovementType movementType, String batchNumber, ProductBatch batch,
                                        String serialNumber, String description) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(qty);
        movement.setMovementType(movementType);
        movement.setBatchNumber(batchNumber);
        movement.setBatch(batch);
        movement.setSerialNumber(serialNumber);
        movement.setDescription(description);
        movement.setMovementDate(LocalDateTime.now());
        movement.setCreatedBy(CurrentUserContext.getUsername());
        return stockMovementRepository.save(movement);
    }

    public void adjustStock(Product product, Warehouse warehouse, BigDecimal delta) {
        Stock stock = stockRepository.findForReservation(product.getId(), warehouse.getId())
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setProduct(product);
                    newStock.setWarehouse(warehouse);
                    newStock.setQuantity(BigDecimal.ZERO);
                    return newStock;
                });
        stock.setQuantity(stock.getQuantity().add(delta));
        stockRepository.save(stock);
    }

    @Transactional
    public StockMovementDTO registerMovement(RegisterMovementRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        String key = request.idempotencyKey();
        String fingerprint = key == null ? null : movementFingerprint(companyId, request);
        if (key != null) {
            var previous = stockMovementOperationRepository.findByCompanyIdAndIdempotencyKey(companyId, key);
            if (previous.isPresent()) {
                if (!fingerprint.equals(previous.get().getRequestFingerprint())) {
                    throw new BusinessRuleException(
                            "A chave de idempotência deste movimento já foi usada com dados diferentes.");
                }
                if (previous.get().getStockMovement() == null) {
                    throw new BusinessRuleException("O movimento anterior ainda está em processamento. Tente novamente.");
                }
                return toDTO(previous.get().getStockMovement());
            }
        }
        Product product = productRepository.findByIdAndCompaniesId(request.productId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        if (!companyId.equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        StockMovementOperation operation = null;
        if (key != null) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
            operation = new StockMovementOperation();
            operation.setCompany(company);
            operation.setIdempotencyKey(key);
            operation.setRequestFingerprint(fingerprint);
            operation.setCreatedBy(CurrentUserContext.getUsername());
            operation = stockMovementOperationRepository.saveAndFlush(operation);
        }
        StockMovement movement = registerMovement(product, warehouse, request.quantity(), request.movementType(),
                request.batchNumber(), request.serialNumber(), request.description(), request.expirationDate());
        if (operation != null) {
            operation.setStockMovement(movement);
            stockMovementOperationRepository.save(operation);
        }
        return movement == null ? null : toDTO(movement);
    }

    @Transactional
    public StockMovementDTO adjustStock(CreateStockAdjustmentRequest request) {
        PermissionGuard.requireManagerOrAdmin("ajustar stock");
        CurrentUserContext.requireCompany(request.companyId());
        String key = request.idempotencyKey();
        String fingerprint = key == null ? null : adjustmentFingerprint(request);
        if (key != null) {
            var previous = stockAdjustmentOperationRepository
                    .findByCompanyIdAndIdempotencyKey(request.companyId(), key);
            if (previous.isPresent()) {
                if (!fingerprint.equals(previous.get().getRequestFingerprint())) {
                    throw new BusinessRuleException(
                            "A chave de idempotência deste ajuste já foi usada com dados diferentes.");
                }
                if (previous.get().getStockMovement() == null) {
                    throw new BusinessRuleException("O ajuste anterior ainda está em processamento. Tente novamente.");
                }
                return toDTO(previous.get().getStockMovement());
            }
        }
        Product product = productRepository.findByIdAndCompaniesId(request.productId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado."));
        if (!product.isStockTracked()) {
            throw new BusinessRuleException("Este produto não controla stock físico.");
        }
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        if (!request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }

        StockAdjustmentOperation operation = null;
        if (key != null) {
            Company company = companyRepository.findById(request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
            operation = new StockAdjustmentOperation();
            operation.setCompany(company);
            operation.setProduct(product);
            operation.setWarehouse(warehouse);
            operation.setIdempotencyKey(key);
            operation.setRequestFingerprint(fingerprint);
            operation.setCreatedBy(CurrentUserContext.getUsername());
            operation = stockAdjustmentOperationRepository.saveAndFlush(operation);
        }

        BigDecimal current = stockRepository.findForReservation(product.getId(), warehouse.getId())
                .map(Stock::getQuantity)
                .orElse(BigDecimal.ZERO);
        BigDecimal delta = request.countedQuantity().subtract(current);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("A contagem informada é igual ao stock actual. Nenhum ajuste foi necessário.");
        }

        StockMovement movement = registerMovement(
                product,
                warehouse,
                delta,
                StockMovementType.ADJUSTMENT.name(),
                null,
                null,
                "Ajuste de stock. Motivo: " + request.reason()
                        + ". Stock anterior: " + current + "; contado: " + request.countedQuantity()
        );
        if (operation != null) {
            operation.setStockMovement(movement);
            stockAdjustmentOperationRepository.save(operation);
        }
        auditLogService.logCurrent("STOCK_ADJUSTMENT",
                "Produto " + product.getSku() + " no armazém " + warehouse.getName()
                        + ". Anterior: " + current + "; contado: " + request.countedQuantity()
                        + "; diferença: " + delta + ". Motivo: " + request.reason());
        return toDTO(movement);
    }

    private static String movementFingerprint(Long companyId, RegisterMovementRequest request) {
        String canonical = companyId + "|" + request.productId() + "|" + request.warehouseId() + "|"
                + request.quantity().stripTrailingZeros().toPlainString() + "|" + request.movementType().trim()
                + "|" + value(request.batchNumber()) + "|" + value(request.serialNumber()) + "|"
                + value(request.description()) + "|" + value(request.expirationDate());
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível no runtime Java.", impossible);
        }
    }

    private static String adjustmentFingerprint(CreateStockAdjustmentRequest request) {
        String canonical = request.companyId() + "|" + request.productId() + "|" + request.warehouseId()
                + "|" + request.countedQuantity().stripTrailingZeros().toPlainString()
                + "|" + request.reason().trim();
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível no runtime Java.", impossible);
        }
    }

    private static String value(Object value) {
        return value == null ? "<null>" : value.toString().trim();
    }

    public StockMovementDTO toDTO(StockMovement m) {
        return new StockMovementDTO(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getName(),
                m.getWarehouse().getId(),
                m.getWarehouse().getName(),
                m.getQuantity(),
                m.getMovementType() != null ? m.getMovementType().name() : null,
                m.getBatchNumber(),
                m.getSerialNumber(),
                m.getDescription(),
                m.getMovementDate(),
                m.getProduct().getBusinessUnit() != null ? m.getProduct().getBusinessUnit().getId() : null,
                m.getProduct().getBusinessUnit() != null ? m.getProduct().getBusinessUnit().getName() : null
        );
    }
}
