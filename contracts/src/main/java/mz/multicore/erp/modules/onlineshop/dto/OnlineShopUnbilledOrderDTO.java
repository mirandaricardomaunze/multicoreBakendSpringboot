package mz.multicore.erp.modules.onlineshop.dto;

import java.time.LocalDateTime;

/**
 * An online order still to be billed, for the desktop bell (docs/LOJA_ONLINE_SPEC.md §3.5, LO-UI-03): number,
 * customer, "Levantamento — {loja}" / "Entrega — {zona}" and when it came in.
 */
public record OnlineShopUnbilledOrderDTO(Long orderId, String number, String customerName, String fulfilmentLabel,
                                         LocalDateTime createdAt) {
}
