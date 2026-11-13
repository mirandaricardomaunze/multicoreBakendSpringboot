package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.dto.RegisterMovementRequest;
import mz.multicore.erp.modules.inventory.dto.StockMovementDTO;
import mz.multicore.erp.modules.inventory.model.*;
import mz.multicore.erp.modules.inventory.repository.StockMovementOperationRepository;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Gestor transacional de registo físico de movimentos de stock e controlo de lotes/FEFO.
 */
@Service
public class StockMovementRegistrationService {

    private final StockMovementRepository stockMovementRepository;
    private final StockMovementOperationRepository stockMovementOperationRepository;
    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductLookup productLookup;
    private final CompanyLookup companyLookup;
    private final ProductBatchService productBatchService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Autowired
    public StockMovementRegistrationService(
            StockMovementRepository stockMovementRepository,
            StockMovementOperationRepository stockMovementOperationRepository,
            StockRepository stockRepository,
            WarehouseRepository warehouseRepository,
            ProductLookup productLookup,
            CompanyLookup companyLookup,
            ProductBatchService productBatchService,
            @Autowired(required = false) org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
        this.stockMovementRepository = stockMovementRepository;
        this.stockMovementOperationRepository = stockMovementOperationRepository;
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
        this.productLookup = productLookup;
        this.companyLookup = companyLookup;
        this.productBatchService = productBatchService;
    }

    @Transactional
    public StockMovement registerMovement(Product product, Warehouse warehouse, BigDecimal quantity,
                                          String movementType, String batchNumber, String serialNumber,
                                          String description, LocalDate expirationDate) {
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

        StockMovement movement = quantity.compareTo(BigDecimal.ZERO) > 0
                ? registerEntry(product, warehouse, quantity, type, batchNumber, serialNumber, description, expirationDate)
                : registerExit(product, warehouse, quantity.negate(), type, serialNumber, description);
        InventoryValueEvents.publish(eventPublisher, product, warehouse, quantity, type, movement, description);
        return movement;
    }

    private StockMovement registerEntry(Product product, Warehouse warehouse, BigDecimal quantity,
                                        StockMovementType movementType, String batchNumber, String serialNumber,
                                        String description, LocalDate expirationDate) {
        requireExpiryForPerishableIntake(product, movementType, expirationDate);
        ProductBatch batch = productBatchService.addToBatch(product, warehouse, batchNumber, expirationDate, quantity);
        StockMovement movement = saveMovement(product, warehouse, quantity, movementType,
                batch.getBatchNumber(), batch, serialNumber, description);
        adjustStock(product, warehouse, quantity);
        return movement;
    }

    /**
     * Stock novo que chega de fora (compra ou entrada manual) de um produto perecível tem de trazer
     * validade. Devoluções, estornos, transferências e acertos repõem stock já existente e ficam de fora.
     */
    static void requireExpiryForPerishableIntake(Product product, StockMovementType movementType,
                                                 LocalDate expirationDate) {
        boolean intake = movementType == StockMovementType.PURCHASE || movementType == StockMovementType.ENTRY;
        if (intake && expirationDate == null && product != null && product.isTracksExpiry()) {
            throw new BusinessRuleException(String.format(
                    "O produto '%s' é perecível: indique a data de validade do lote. "
                            + "Numa encomenda, edite a linha e preencha a validade antes de receber.",
                    product.getName()));
        }
    }

    private StockMovement registerExit(Product product, Warehouse warehouse, BigDecimal absQty,
                                       StockMovementType movementType, String serialNumber, String description) {
        ensureLegacyBatchIfNeeded(product, warehouse);
        List<ProductBatchService.BatchConsumption> debits = productBatchService.consumeFEFO(product, warehouse, absQty);
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
    public StockMovementDTO registerMovementWithIdempotency(RegisterMovementRequest request, StockMovementQueryService queryService) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        String key = request.idempotencyKey();
        String fingerprint = key == null ? null : movementFingerprint(companyId, request);
        if (key != null) {
            var previous = stockMovementOperationRepository.findByCompanyIdAndIdempotencyKey(companyId, key);
            if (previous.isPresent()) {
                if (!fingerprint.equals(previous.get().getRequestFingerprint())) {
                    throw new BusinessRuleException("A chave de idempotência deste movimento já foi usada com dados diferentes.");
                }
                if (previous.get().getStockMovement() == null) {
                    throw new BusinessRuleException("O movimento anterior ainda está em processamento. Tente novamente.");
                }
                return queryService.toDTO(previous.get().getStockMovement());
            }
        }
        Product product = productLookup.requireForCompany(request.productId(), companyId);
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        if (!companyId.equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }
        StockMovementOperation operation = null;
        if (key != null) {
            Company company = companyLookup.require(companyId);
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
        return movement == null ? null : queryService.toDTO(movement);
    }

    private static String movementFingerprint(Long companyId, RegisterMovementRequest request) {
        String canonical = companyId + "|" + request.productId() + "|" + request.warehouseId() + "|"
                + request.quantity().stripTrailingZeros().toPlainString() + "|" + request.movementType().trim()
                + "|" + value(request.batchNumber()) + "|" + value(request.serialNumber()) + "|"
                + value(request.description()) + "|" + value(request.expirationDate());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível no runtime Java.", impossible);
        }
    }

    private static String value(Object value) {
        return value == null ? "<null>" : value.toString().trim();
    }
}
