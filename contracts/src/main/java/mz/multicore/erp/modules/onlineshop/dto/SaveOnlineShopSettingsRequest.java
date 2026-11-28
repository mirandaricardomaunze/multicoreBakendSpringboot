package mz.multicore.erp.modules.onlineshop.dto;

import java.util.List;

/**
 * Online shop settings of the active company, saved by the ADMIN (docs/LOJA_ONLINE_SPEC.md §3.1, LO-01).
 * {@code autoCancelDays} null means the default of 7 days.
 */
public record SaveOnlineShopSettingsRequest(boolean enabled,
                                            String publicCode,
                                            List<Long> pickupWarehouseIds,
                                            List<OnlineShopDeliveryZoneDTO> deliveryZones,
                                            String contactPhone,
                                            Integer autoCancelDays) {
}
