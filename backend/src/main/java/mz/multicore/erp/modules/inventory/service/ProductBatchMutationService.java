package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.ProductBatch;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import mz.multicore.erp.modules.inventory.service.ProductBatchService.BatchConsumption;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProductBatchMutationService {

    private static final LocalDate LEGACY_EXPIRATION = LocalDate.of(9999, 12, 31);
    private static final String LEGACY_BATCH_PREFIX = "LEGACY-";

    private final ProductBatchRepository batchRepository;

    public ProductBatchMutationService(ProductBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public ProductBatch addToBatch(Product product, Warehouse warehouse, String batchNumber,
                                   LocalDate expirationDate, BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Quantidade de entrada deve ser positiva.");
        }

        if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException(String.format(
                    "Não é possível dar entrada de stock de '%s' com lote já vencido (validade %s).",
                    product.getName(), expirationDate));
        }

        String effectiveBatch = (batchNumber == null || batchNumber.isBlank())
                ? defaultBatchNumber(expirationDate)
                : batchNumber;
        LocalDate effectiveExpiration = expirationDate != null ? expirationDate : LEGACY_EXPIRATION;

        ProductBatch batch = batchRepository
                .findByProductIdAndWarehouseIdAndBatchNumber(product.getId(), warehouse.getId(), effectiveBatch)
                .orElseGet(() -> {
                    ProductBatch nb = new ProductBatch();
                    nb.setProduct(product);
                    nb.setWarehouse(warehouse);
                    nb.setBatchNumber(effectiveBatch);
                    nb.setExpirationDate(effectiveExpiration);
                    nb.setEntryDate(LocalDate.now());
                    nb.setQuantity(BigDecimal.ZERO);
                    return nb;
                });

        batch.setQuantity(batch.getQuantity().add(quantity));
        return batchRepository.save(batch);
    }

    public List<BatchConsumption> consumeFEFO(Product product, Warehouse warehouse, BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Quantidade de saída deve ser positiva.");
        }

        List<ProductBatch> available = batchRepository.findAvailableFEFO(product.getId(), warehouse.getId());
        BigDecimal totalAvailable = available.stream()
                .map(ProductBatch::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAvailable.compareTo(quantity) < 0) {
            throw new BusinessRuleException(String.format(
                    "Stock insuficiente de '%s' no armazém '%s'. Requerido: %s, Disponível em lotes: %s",
                    product.getName(), warehouse.getName(), quantity, totalAvailable));
        }

        List<BatchConsumption> debits = new ArrayList<>();
        BigDecimal remaining = quantity;

        for (ProductBatch batch : available) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal take = batch.getQuantity().min(remaining);
            batch.setQuantity(batch.getQuantity().subtract(take));
            batchRepository.save(batch);

            debits.add(new BatchConsumption(batch, take));
            remaining = remaining.subtract(take);
        }

        return debits;
    }

    public ProductBatch ensureLegacyBatch(Product product, Warehouse warehouse, BigDecimal quantity) {
        String legacyBatch = LEGACY_BATCH_PREFIX + product.getId();
        ProductBatch batch = batchRepository
                .findByProductIdAndWarehouseIdAndBatchNumber(product.getId(), warehouse.getId(), legacyBatch)
                .orElseGet(() -> {
                    ProductBatch nb = new ProductBatch();
                    nb.setProduct(product);
                    nb.setWarehouse(warehouse);
                    nb.setBatchNumber(legacyBatch);
                    nb.setExpirationDate(LEGACY_EXPIRATION);
                    nb.setEntryDate(LocalDate.now());
                    nb.setQuantity(BigDecimal.ZERO);
                    return nb;
                });
        batch.setQuantity(quantity);
        return batchRepository.save(batch);
    }

    public void materialiseLegacyIfNeeded(Product product, Warehouse warehouse, BigDecimal stockQuantity) {
        if (stockQuantity == null) return;
        BigDecimal batchTotal = batchRepository.sumQuantityByProductAndWarehouse(product.getId(), warehouse.getId());
        if (batchTotal == null) batchTotal = BigDecimal.ZERO;
        if (stockQuantity.compareTo(batchTotal) > 0) {
            ensureLegacyBatch(product, warehouse, stockQuantity);
        }
    }

    private String defaultBatchNumber(LocalDate expirationDate) {
        if (expirationDate == null) {
            return "AUTO-" + System.currentTimeMillis();
        }
        return "EXP-" + expirationDate;
    }
}
