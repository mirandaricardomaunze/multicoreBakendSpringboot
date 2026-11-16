package mz.multicore.erp.modules.returnables.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaveReturnableItemRequest(@NotNull @Positive Long companyId, @NotNull @Positive Long productId, boolean active) {}
