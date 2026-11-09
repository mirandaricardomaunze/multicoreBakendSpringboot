package mz.multicore.erp.modules.returnables.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Invoice the client for returnables that will not come back (lost or broken). */
public record ChargeReturnablesRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive Long clientId,
        @NotNull @Positive Long itemId,
        @NotNull @Positive BigDecimal quantity,
        @Size(max = 300) String notes,
        @NotBlank @Size(max = 64) String requestKey
) {}
