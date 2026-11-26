package mz.multicore.erp.modules.comercial.model;

import java.math.BigDecimal;

/** Quantity of a product held by active order reservations in one warehouse (query projection, not an entity). */
public record ReservedStock(Long productId, Long warehouseId, BigDecimal quantity) {
}
