package com.phcpro.modules.inventory.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.inventory.dto.ProductBatchDTO;
import com.phcpro.modules.inventory.model.ProductBatch;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.ProductBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductBatchService {

    private static final LocalDate LEGACY_EXPIRATION = LocalDate.of(9999, 12, 31);
    private static final String LEGACY_BATCH_PREFIX = "LEGACY-";

    private final ProductBatchRepository batchRepository;

    public ProductBatchService(ProductBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @Transactional
    public ProductBatch addToBatch(Product product, Warehouse warehouse, String batchNumber,
                                    LocalDate expirationDate, BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Quantidade de entrada deve ser positiva.");
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

    /**
     * Consome quantidade do produto/armazém usando FEFO. Pode atravessar vários lotes.
     * Retorna a lista de débitos efetuados (um por lote consumido). Atomic: se não houver stock
     * suficiente, lança exceção sem alterar nada.
     */
    @Transactional
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

    /**
     * Garante que existe um lote LEGACY com a quantidade indicada (usado para migração de stocks
     * antigos que não tinham rastreio de lote).
     */
    @Transactional
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

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findByCompany(Long companyId) {
        return batchRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findByWarehouse(Long warehouseId) {
        return batchRepository.findByWarehouseId(warehouseId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductBatchDTO> findExpiringByCompany(Long companyId, LocalDate cutoff) {
        return batchRepository.findExpiringByCompanyId(companyId, cutoff).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal sumQuantity(Long productId, Long warehouseId) {
        return batchRepository.sumQuantityByProductAndWarehouse(productId, warehouseId);
    }

    public ProductBatchDTO toDTO(ProductBatch b) {
        return new ProductBatchDTO(
                b.getId(),
                b.getProduct().getId(),
                b.getProduct().getSku(),
                b.getProduct().getName(),
                b.getWarehouse().getId(),
                b.getWarehouse().getName(),
                b.getBatchNumber(),
                b.getExpirationDate(),
                b.getEntryDate(),
                b.getQuantity()
        );
    }

    private String defaultBatchNumber(LocalDate expirationDate) {
        if (expirationDate == null) {
            return "AUTO-" + System.currentTimeMillis();
        }
        return "EXP-" + expirationDate;
    }

    public record BatchConsumption(ProductBatch batch, BigDecimal quantity) {}
}
