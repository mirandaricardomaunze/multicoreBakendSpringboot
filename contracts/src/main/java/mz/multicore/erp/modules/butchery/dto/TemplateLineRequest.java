package mz.multicore.erp.modules.butchery.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record TemplateLineRequest(
        @NotNull @Positive Long productId,
        @NotNull @PositiveOrZero @DecimalMax("100") BigDecimal expectedYieldPct
) {}
