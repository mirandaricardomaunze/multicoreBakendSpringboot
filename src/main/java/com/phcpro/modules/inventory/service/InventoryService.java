package com.phcpro.modules.inventory.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.inventory.dto.CreateStockAdjustmentRequest;
import com.phcpro.modules.inventory.dto.CreateWarehouseRequest;
import com.phcpro.modules.inventory.dto.StockDTO;
import com.phcpro.modules.inventory.dto.StockMovementDTO;
import com.phcpro.modules.inventory.dto.WarehouseDTO;
import com.phcpro.modules.inventory.model.ProductBatch;
import com.phcpro.modules.inventory.model.Stock;
import com.phcpro.modules.inventory.model.StockMovement;
import com.phcpro.modules.inventory.model.StockMovementType;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.StockMovementRepository;
import com.phcpro.modules.inventory.repository.StockRepository;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryService {

    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CompanyRepository companyRepository;
    private final ProductBatchService productBatchService;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public InventoryService(
            WarehouseRepository warehouseRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            CompanyRepository companyRepository,
            ProductBatchService productBatchService,
            ProductRepository productRepository,
            AuditLogService auditLogService
    ) {
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.companyRepository = companyRepository;
        this.productBatchService = productBatchService;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return warehouseRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<Stock> getStocksByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return stockRepository.findByWarehouseCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<Stock> getStocksByWarehouse(Long warehouseId) {
        return stockRepository.findByWarehouseId(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getMovementsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return stockMovementRepository.findByCompanyId(companyId);
    }

    @Transactional
    public Warehouse createWarehouse(String name, String location, Company company) {
        return createWarehouse(name, null, null, location, company);
    }

    @Transactional
    public Warehouse createWarehouse(String name, String warehouseNumber, BigDecimal capacity, String location, Company company) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(name);
        warehouse.setWarehouseNumber(warehouseNumber);
        warehouse.setCapacity(capacity);
        warehouse.setLocation(location);
        warehouse.setCompany(company);
        warehouse.setCreatedBy("SYSTEM");
        return warehouseRepository.save(warehouse);
    }

    /**
     * Registro de movimento sem validade explícita (compatibilidade).
     * Entradas (qty>0) vão para um lote derivado do batchNumber (ou AUTO se vazio) com validade
     * 9999-12-31. Saídas (qty<0) usam FEFO.
     */
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

    /**
     * Registro de movimento com validade.
     * - Entradas (qty>0): debita/cria lote (product, warehouse, batchNumber, expirationDate).
     *   Se batchNumber for null/vazio, é derivado da validade (EXP-yyyy-mm-dd).
     * - Saídas (qty<0): aplica FEFO — escolhe lote(s) com validade mais próxima. Pode gerar
     *   múltiplos StockMovement (um por lote consumido). batchNumber/expirationDate são ignorados.
     */
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
        // Migração lazy: se houver Stock antigo sem lotes correspondentes, cria um LEGACY.
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
        BigDecimal batchTotal = productBatchService.sumQuantity(product.getId(), warehouse.getId());
        Stock stock = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElse(null);
        if (stock == null) return;

        BigDecimal gap = stock.getQuantity().subtract(batchTotal);
        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            productBatchService.ensureLegacyBatch(product, warehouse, batchTotal.add(gap));
        }
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

    private void adjustStock(Product product, Warehouse warehouse, BigDecimal delta) {
        Stock stock = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
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
    public WarehouseDTO createWarehouse(CreateWarehouseRequest request) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        Warehouse warehouse = createWarehouse(
                request.name(),
                request.warehouseNumber(),
                request.capacity(),
                request.location(),
                company
        );
        return toDTO(warehouse);
    }

    @Transactional
    public StockMovementDTO adjustStock(CreateStockAdjustmentRequest request) {
        PermissionGuard.requireManagerOrAdmin("ajustar stock");
        CurrentUserContext.requireCompany(request.companyId());
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

        BigDecimal current = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
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
        auditLogService.logCurrent("STOCK_ADJUSTMENT",
                "Produto " + product.getSku() + " no armazém " + warehouse.getName()
                        + ". Anterior: " + current + "; contado: " + request.countedQuantity()
                        + "; diferença: " + delta + ". Motivo: " + request.reason());
        return toDTO(movement);
    }

    @Transactional(readOnly = true)
    public List<WarehouseDTO> findWarehousesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return warehouseRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<StockDTO> findStocksByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return stockRepository.findByWarehouseCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<StockDTO> findStocksByWarehouse(Long warehouseId) {
        return stockRepository.findByWarehouseId(warehouseId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> findMovementsByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return stockMovementRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<com.phcpro.modules.inventory.dto.ProductBatchDTO> findBatchesByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return productBatchService.findByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<com.phcpro.modules.inventory.dto.ProductBatchDTO> findNextFEFO(Long productId, Long warehouseId) {
        return productBatchService.findNextFEFO(productId, warehouseId);
    }

    public WarehouseDTO toDTO(Warehouse w) {
        return new WarehouseDTO(
                w.getId(),
                w.getName(),
                w.getLocation(),
                w.getWarehouseNumber(),
                w.getCapacity(),
                w.getCompany() != null ? w.getCompany().getId() : null
        );
    }

    public StockDTO toDTO(Stock s) {
        return new StockDTO(
                s.getId(),
                s.getProduct().getId(),
                s.getProduct().getSku(),
                s.getProduct().getReference(),
                s.getProduct().getBarcode(),
                s.getProduct().getName(),
                s.getWarehouse().getId(),
                s.getWarehouse().getName(),
                s.getQuantity(),
                s.getProduct().getMinStock() != null ? s.getProduct().getMinStock() : BigDecimal.ZERO
        );
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
                m.getMovementDate()
        );
    }
}
