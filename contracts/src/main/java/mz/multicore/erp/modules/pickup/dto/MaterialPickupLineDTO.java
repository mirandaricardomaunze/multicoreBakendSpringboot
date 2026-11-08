package mz.multicore.erp.modules.pickup.dto;

import java.math.BigDecimal;

/** Quantity of one invoice line handed over on a pickup slip. */
public record MaterialPickupLineDTO(
        Long invoiceLineId,
        String productSku,
        String productName,
        String unitCode,
        BigDecimal quantity
) {}
