package mz.multicore.erp.modules.units.dto;

import java.math.BigDecimal;

/**
 * Alternative unit of a product: one {@code unitCode} equals {@code factorToBase} base units.
 * {@code derived=true} marks rows that come from the product box/package composition; those can only be
 * changed on the product record itself.
 */
public record ProductUnitDTO(
        Long id,
        String unitCode,
        String unitName,
        String unitSymbol,
        String dimension,
        BigDecimal factorToBase,
        String barcode,
        boolean sellable,
        boolean purchasable,
        boolean defaultSale,
        boolean defaultPurchase,
        boolean derived
) {}
