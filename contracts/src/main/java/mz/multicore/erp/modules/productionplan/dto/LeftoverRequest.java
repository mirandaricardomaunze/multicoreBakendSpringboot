package mz.multicore.erp.modules.productionplan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Unsold quantity at closing; {@code writeOff} sends it to the stock waste flow, otherwise it stays for tomorrow. */
public record LeftoverRequest(@NotNull @Positive Long planLineId, @NotNull @PositiveOrZero BigDecimal quantity, boolean writeOff) {}
