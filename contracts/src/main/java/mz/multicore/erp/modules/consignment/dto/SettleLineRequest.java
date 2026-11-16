package mz.multicore.erp.modules.consignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** What came back: in good condition (back to the shop) and damaged or stale (written off). */
public record SettleLineRequest(@NotNull @Positive Long lineId, @NotNull @PositiveOrZero BigDecimal returned,
                                @NotNull @PositiveOrZero BigDecimal damaged) {}
