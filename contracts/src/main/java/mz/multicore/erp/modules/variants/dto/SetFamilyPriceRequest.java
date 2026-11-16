package mz.multicore.erp.modules.variants.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** One sale price for every variant of the family. */
public record SetFamilyPriceRequest(@NotNull @Positive Long companyId, @NotNull @Positive BigDecimal unitPrice) {}
