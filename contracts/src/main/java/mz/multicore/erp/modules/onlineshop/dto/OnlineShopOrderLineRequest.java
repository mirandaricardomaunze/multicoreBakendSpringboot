package mz.multicore.erp.modules.onlineshop.dto;

import java.math.BigDecimal;

/** Cart line of an online order: SKU and quantity only — prices always come from the system (§3.3). */
public record OnlineShopOrderLineRequest(String sku, BigDecimal quantity) {
}
