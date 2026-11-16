package mz.multicore.erp.modules.productionplan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record PlanLineRequest(@NotNull @Positive Long bomId, @NotNull @PositiveOrZero BigDecimal plannedQty) {}
