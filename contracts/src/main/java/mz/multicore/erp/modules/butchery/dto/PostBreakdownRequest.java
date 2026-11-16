package mz.multicore.erp.modules.butchery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Post a carcass breakdown: the carcass leaves stock and the cuts enter with their share of the cost.
 * {@code inputCost} is the total paid for this carcass; when empty, the carcass cost price times the weight.
 * {@code requestKey} makes a retry return the same breakdown.
 */
public record PostBreakdownRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive Long warehouseId,
        @Positive Long templateId,
        @NotNull @Positive Long sourceProductId,
        @NotNull @Positive BigDecimal inputKg,
        @PositiveOrZero BigDecimal inputCost,
        @Size(max = 120) String reference,
        @Size(max = 300) String notes,
        @NotEmpty @Valid List<BreakdownLineRequest> lines,
        @NotBlank @Size(max = 64) String requestKey
) {}
