package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.modules.onlineshop.dto.OnlineShopFulfilment;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderLineRequest;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopDeliveryZone;

import java.util.List;

/**
 * Checkout already validated by {@link OnlineShopCheckoutRules} (LO-05): trimmed values, phone in national format
 * (9 digits), NUIT null when not given; pickup store only for {@code LEVANTAMENTO}, zone and address only for
 * {@code ENTREGA}.
 */
record OnlineShopCheckout(String customerName,
                          String phone,
                          String nuit,
                          OnlineShopFulfilment fulfilment,
                          Long pickupWarehouseId,
                          OnlineShopDeliveryZone deliveryZone,
                          String deliveryAddress,
                          String notes,
                          List<OnlineShopOrderLineRequest> lines) {
}
