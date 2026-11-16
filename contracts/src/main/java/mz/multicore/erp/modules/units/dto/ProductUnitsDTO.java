package mz.multicore.erp.modules.units.dto;

import java.util.List;

/**
 * Units of a product. The base unit is the unit of stock, prices and documents.
 * {@code baseUnitLocked=true} once the product has movements or alternative units, because changing the base
 * would alter the meaning of the existing stock.
 */
public record ProductUnitsDTO(
        Long productId,
        String productName,
        UnitOfMeasureDTO baseUnit,
        boolean baseUnitLocked,
        String baseUnitLockReason,
        List<ProductUnitDTO> units
) {}
