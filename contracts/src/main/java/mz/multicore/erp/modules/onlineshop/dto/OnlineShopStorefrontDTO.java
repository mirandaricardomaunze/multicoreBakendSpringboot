package mz.multicore.erp.modules.onlineshop.dto;

import java.util.List;

/**
 * What the public page of the online shop needs to show the shop and its checkout (docs/LOJA_ONLINE_SPEC.md §3.2–3.3,
 * LO-11): the company's display name, the contact phone, the pickup stores and the delivery zones with their fee —
 * nothing internal (no company id, prazo or warehouse data beyond the name).
 */
public record OnlineShopStorefrontDTO(String name,
                                      String contactPhone,
                                      List<OnlineShopPickupStoreDTO> pickupStores,
                                      List<OnlineShopDeliveryZoneDTO> deliveryZones) {
}
