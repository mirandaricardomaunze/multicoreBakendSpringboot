package mz.multicore.erp.modules.crm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Nova tarifa horária da assistência técnica. */
public record UpdateCrmSettingsRequest(
    @NotNull(message = "A tarifa horária é obrigatória.")
    @Positive(message = "A tarifa horária tem de ser maior que zero.")
    BigDecimal hourlyRate
) {}
