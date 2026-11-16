package mz.multicore.erp.modules.butchery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** Create ({@code id == null}) or update a breakdown model. */
public record SaveBreakdownTemplateRequest(
        Long id,
        @NotNull @Positive Long companyId,
        @NotBlank @Size(max = 120) String name,
        @NotNull @Positive Long sourceProductId,
        @NotNull @PositiveOrZero @DecimalMax("100") BigDecimal lossTolerancePct,
        boolean active,
        @NotEmpty @Valid List<TemplateLineRequest> lines
) {}
