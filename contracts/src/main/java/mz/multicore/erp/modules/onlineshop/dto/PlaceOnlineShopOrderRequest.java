package mz.multicore.erp.modules.onlineshop.dto;

import java.util.List;

/**
 * Checkout of the public online shop (docs/LOJA_ONLINE_SPEC.md §3.3, LO-05): name and Mozambican mobile phone are
 * required, NUIT optional; {@code LEVANTAMENTO} needs one of the shop's pickup stores, {@code ENTREGA} a delivery zone
 * and an address.
 */
public record PlaceOnlineShopOrderRequest(String customerName,
                                          String phone,
                                          String nuit,
                                          OnlineShopFulfilment fulfilment,
                                          Long pickupWarehouseId,
                                          String deliveryZone,
                                          String deliveryAddress,
                                          String notes,
                                          List<OnlineShopOrderLineRequest> lines) {
}
