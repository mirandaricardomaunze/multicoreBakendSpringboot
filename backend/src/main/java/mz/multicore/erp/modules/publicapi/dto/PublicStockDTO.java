package mz.multicore.erp.modules.publicapi.dto;

import java.math.BigDecimal;

/** Stock available for sale of one product in one sales warehouse (quantity − active reservations, ≥ 0). */
public record PublicStockDTO(String sku, String warehouseCode, String warehouseName, BigDecimal available) {
}
