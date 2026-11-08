package mz.multicore.erp.modules.advances.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Apply an open advance to the final invoice of the same client. */
public record ApplyAdvanceRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive Long invoiceId
) {}
