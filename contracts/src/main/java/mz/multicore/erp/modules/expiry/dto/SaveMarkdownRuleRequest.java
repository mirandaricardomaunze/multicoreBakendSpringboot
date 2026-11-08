package mz.multicore.erp.modules.expiry.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SaveMarkdownRuleRequest(
        Long id,
        @NotNull @Positive Long companyId,
        @Positive Long categoryId,
        @Min(0) @Max(60) int daysBefore,
        @NotNull @DecimalMin("1") @DecimalMax("90") BigDecimal percent,
        boolean active
) {}
