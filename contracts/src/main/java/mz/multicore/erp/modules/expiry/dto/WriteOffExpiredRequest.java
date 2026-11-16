package mz.multicore.erp.modules.expiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Write off an expired lot through the ordinary stock waste flow (reason: expired). */
public record WriteOffExpiredRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive Long batchId,
        @Size(max = 300) String notes,
        @NotBlank @Size(max = 60) String requestKey
) {}
