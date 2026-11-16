package mz.multicore.erp.modules.expiry.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Mark down one lot now: a discount on the product until the lot expires. */
public record MarkdownRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive Long batchId,
        @NotNull @DecimalMin("1") @DecimalMax("90") BigDecimal percent
) {}
