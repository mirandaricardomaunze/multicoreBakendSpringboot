package mz.multicore.erp.modules.variants.dto;

import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Create a family and one product per combination. The template product gives price, cost, category, VAT and
 * stock rules to every variant. {@code attribute2} and {@code values2} are optional (one-dimension families).
 */
public record CreateVariantFamilyRequest(
        @NotNull @Positive Long companyId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9-]+") String skuPrefix,
        @NotNull @Positive Long templateProductId,
        @NotBlank @Size(max = 40) String attribute1,
        @NotEmpty List<@NotBlank @Size(max = 30) String> values1,
        @Size(max = 40) String attribute2,
        List<@NotBlank @Size(max = 30) String> values2
) {}
