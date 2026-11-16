package mz.multicore.erp.modules.weighbridge.dto;

import jakarta.validation.constraints.*;
import mz.multicore.erp.modules.weighbridge.model.WeighingDirection;

import java.math.BigDecimal;

/** Truck at the entrance. {@code fromScale} false means the weight was typed. */
public record FirstWeighingRequest(
        @NotNull @Positive Long companyId,
        @NotBlank @Size(max = 20) String plate,
        @Size(max = 120) String driver,
        @NotNull WeighingDirection direction,
        @Size(max = 160) String counterparty,
        @Positive Long productId,
        @Size(max = 80) String reference,
        @NotNull @Positive BigDecimal weightKg,
        boolean fromScale,
        @Size(max = 300) String notes,
        @NotBlank @Size(max = 64) String requestKey
) {}
