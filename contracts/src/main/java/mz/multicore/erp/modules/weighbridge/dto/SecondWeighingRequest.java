package mz.multicore.erp.modules.weighbridge.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Truck at the exit. */
public record SecondWeighingRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive BigDecimal weightKg,
        boolean fromScale,
        @Size(max = 80) String reference
) {}
