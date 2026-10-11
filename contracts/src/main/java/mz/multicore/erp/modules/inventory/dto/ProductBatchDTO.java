package mz.multicore.erp.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductBatchDTO(
        Long id,
        Long productId,
        String sku,
        String productName,
        Long warehouseId,
        String warehouseName,
        String batchNumber,
        LocalDate expirationDate,
        LocalDate entryDate,
        BigDecimal quantity,
        Long businessUnitId,
        String businessUnitName
) {
    /** Construtor retrocompatível para 10 argumentos. */
    public ProductBatchDTO(
            Long id, Long productId, String sku, String productName,
            Long warehouseId, String warehouseName, String batchNumber,
            LocalDate expirationDate, LocalDate entryDate, BigDecimal quantity
    ) {
        this(id, productId, sku, productName, warehouseId, warehouseName,
                batchNumber, expirationDate, entryDate, quantity, null, null);
    }
}
