package mz.multicore.erp.modules.inventory.dto;

import java.math.BigDecimal;

/** Produto em ruptura de stock (esgotado): controla stock e tem saldo total ≤ 0 na empresa. */
public record StockAlertDTO(
        Long productId,
        String sku,
        String name,
        BigDecimal currentStock,
        Long businessUnitId,
        String businessUnitName
) {
    public StockAlertDTO(Long productId, String sku, String name, BigDecimal currentStock) {
        this(productId, sku, name, currentStock, null, null);
    }
}
