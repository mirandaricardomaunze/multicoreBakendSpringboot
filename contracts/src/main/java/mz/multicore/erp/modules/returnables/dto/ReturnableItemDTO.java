package mz.multicore.erp.modules.returnables.dto;

import java.math.BigDecimal;

/** A product handled as a returnable (crate, bottle, gas bottle, pallet); its price is what is charged if lost. */
public record ReturnableItemDTO(Long id, Long productId, String productName, String sku, BigDecimal chargeValue,
                                boolean active, BigDecimal withClients) {}
