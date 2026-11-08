package mz.multicore.erp.modules.pickup.dto;

import java.math.BigDecimal;

/**
 * Balance of one invoice line: invoiced, credited by approved credit notes, already picked up, and what
 * remains to be collected. Quantities are in the product base unit.
 */
public record PickupBalanceLineDTO(
        Long invoiceLineId,
        Long productId,
        String productSku,
        String productName,
        String unitCode,
        BigDecimal invoiced,
        BigDecimal credited,
        BigDecimal picked,
        BigDecimal balance
) {}
