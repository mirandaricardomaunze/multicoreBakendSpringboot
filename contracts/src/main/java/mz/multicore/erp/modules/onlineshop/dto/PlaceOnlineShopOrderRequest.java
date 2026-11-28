package mz.multicore.erp.modules.onlineshop.dto;

import java.util.List;

/**
 * Checkout of the public online shop (docs/LOJA_ONLINE_SPEC.md §3.3, LO-05): name and Mozambican mobile phone are
 * required, NUIT optional; {@code LEVANTAMENTO} needs one of the shop's pickup stores, {@code ENTREGA} a delivery zone
 * and an address.
 *
 * <p>{@code website} is the honeypot of the public form (§3.2, LO-04): hidden from people, left empty by them; a robot
 * that fills it gets the order refused before anything is looked up or created.
 */
public record PlaceOnlineShopOrderRequest(String customerName,
                                          String phone,
                                          String nuit,
                                          OnlineShopFulfilment fulfilment,
                                          Long pickupWarehouseId,
                                          String deliveryZone,
                                          String deliveryAddress,
                                          String notes,
                                          List<OnlineShopOrderLineRequest> lines,
                                          String website) {

    /** Checkout without the honeypot field (callers other than the public form). */
    public PlaceOnlineShopOrderRequest(String customerName, String phone, String nuit, OnlineShopFulfilment fulfilment,
                                       Long pickupWarehouseId, String deliveryZone, String deliveryAddress,
                                       String notes, List<OnlineShopOrderLineRequest> lines) {
        this(customerName, phone, nuit, fulfilment, pickupWarehouseId, deliveryZone, deliveryAddress, notes, lines,
                null);
    }

    /** Whether the hidden honeypot field came filled — only robots fill it. */
    public boolean honeypotFilled() {
        return website != null && !website.isBlank();
    }
}
