package mz.multicore.erp.modules.inventory.dto;

import java.math.BigDecimal;

/**
 * Representa a contagem individual de um produto numa sessão de inventário.
 */
public record InventoryItemDTO(
        Long id,
        Long productId,
        String productCode,
        String productName,
        String barcode,
        BigDecimal expectedQuantity,
        BigDecimal countedQuantity,
        BigDecimal difference,
        BigDecimal unitCost,
        BigDecimal financialImpact,
        String notes
) {
    public InventoryItemDTO {
        if (expectedQuantity == null) expectedQuantity = BigDecimal.ZERO;
        if (countedQuantity == null) countedQuantity = BigDecimal.ZERO;
        if (difference == null) difference = countedQuantity.subtract(expectedQuantity);
        if (unitCost == null) unitCost = BigDecimal.ZERO;
        if (financialImpact == null) financialImpact = difference.multiply(unitCost);
    }
}
