package mz.multicore.erp.modules.variants.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** New values for one or both attributes; only the missing combinations are created. */
public record AddVariantValuesRequest(
        @NotNull @Positive Long companyId,
        List<@Size(max = 30) String> values1,
        List<@Size(max = 30) String> values2
) {}
