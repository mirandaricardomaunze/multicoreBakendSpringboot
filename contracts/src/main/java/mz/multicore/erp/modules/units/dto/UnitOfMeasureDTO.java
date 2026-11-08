package mz.multicore.erp.modules.units.dto;

import java.math.BigDecimal;

/**
 * Catalogue unit of measure. {@code dimensionFactor} converts to the dimension reference unit
 * (g = 0.001 kg) and is {@code null} for packages. {@code system=true} marks read-only units provided by
 * Multicore.
 */
public record UnitOfMeasureDTO(
        Long id,
        String code,
        String name,
        String symbol,
        String dimension,
        String dimensionLabel,
        BigDecimal dimensionFactor,
        int decimals,
        boolean system
) {}
