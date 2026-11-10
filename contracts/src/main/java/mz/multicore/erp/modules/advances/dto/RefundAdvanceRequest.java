package mz.multicore.erp.modules.advances.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Give an open advance back to the client who gave up the order (managers, reason required). */
public record RefundAdvanceRequest(
        @NotNull @Positive Long companyId,
        @NotBlank @Size(max = 300) String reason
) {}
