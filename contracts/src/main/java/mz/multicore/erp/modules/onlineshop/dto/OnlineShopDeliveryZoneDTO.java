package mz.multicore.erp.modules.onlineshop.dto;

import java.math.BigDecimal;

/** Delivery zone of the online shop: name and fixed fee ≥ 0 (docs/LOJA_ONLINE_SPEC.md §3.1, LO-01). */
public record OnlineShopDeliveryZoneDTO(String name, BigDecimal fee) {
}
