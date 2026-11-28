package mz.multicore.erp.modules.onlineshop.dto;

import java.util.List;

/** Online shop settings of a company (docs/LOJA_ONLINE_SPEC.md §3.1, LO-01). */
public record OnlineShopSettingsDTO(boolean enabled,
                                    String publicCode,
                                    List<Long> pickupWarehouseIds,
                                    List<OnlineShopDeliveryZoneDTO> deliveryZones,
                                    String contactPhone,
                                    int autoCancelDays) {
}
