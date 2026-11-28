package mz.multicore.erp.modules.onlineshop.dto;

import java.math.BigDecimal;

/** One line of the order summary shown to the online customer (docs/LOJA_ONLINE_SPEC.md §3.3). */
public record OnlineShopOrderLineDTO(String sku, String name, BigDecimal quantity) {
}
