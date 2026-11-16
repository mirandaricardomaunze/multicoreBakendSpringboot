package mz.multicore.erp.modules.butchery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Weight actually obtained for one cut, in kg. */
public record BreakdownLineRequest(
        @NotNull @Positive Long productId,
        @NotNull @Positive BigDecimal kg
) {}
