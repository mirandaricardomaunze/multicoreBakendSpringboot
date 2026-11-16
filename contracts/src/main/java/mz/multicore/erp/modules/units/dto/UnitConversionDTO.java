package mz.multicore.erp.modules.units.dto;

import java.math.BigDecimal;

/** Result of converting {@code quantity} from {@code unitCode} to the product base unit. */
public record UnitConversionDTO(
        Long productId,
        String unitCode,
        BigDecimal quantity,
        BigDecimal factorToBase,
        String baseUnitCode,
        BigDecimal baseQuantity
) {}
