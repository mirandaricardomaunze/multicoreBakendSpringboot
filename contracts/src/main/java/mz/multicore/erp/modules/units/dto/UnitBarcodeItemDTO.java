package mz.multicore.erp.modules.units.dto;

import mz.multicore.erp.modules.comercial.dto.POSCatalogItemDTO;

import java.math.BigDecimal;

/**
 * A barcode that belongs to an alternative unit of a product (a pallet, a bag): reading it sells
 * {@code factorToBase} base units of {@code item}. Stock and the sale line stay in the base unit.
 */
public record UnitBarcodeItemDTO(
        POSCatalogItemDTO item,
        String unitCode,
        String unitName,
        BigDecimal factorToBase
) {}
