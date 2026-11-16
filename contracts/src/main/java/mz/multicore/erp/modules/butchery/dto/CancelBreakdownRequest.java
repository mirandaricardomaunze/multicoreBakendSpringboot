package mz.multicore.erp.modules.butchery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CancelBreakdownRequest(
        @NotNull @Positive Long companyId,
        @NotBlank @Size(max = 300) String reason
) {}
