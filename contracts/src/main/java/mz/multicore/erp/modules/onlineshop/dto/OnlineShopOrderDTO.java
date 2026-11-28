package mz.multicore.erp.modules.onlineshop.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Answer to the online checkout (docs/LOJA_ONLINE_SPEC.md §3.3, LO-06): the order number the customer uses to follow
 * the order, its state and a summary with the system's VAT-inclusive total — no ids, costs or reservations.
 */
public record OnlineShopOrderDTO(String number, String statusLabel, List<OnlineShopOrderLineDTO> lines,
                                 BigDecimal totalAmount) {
}
