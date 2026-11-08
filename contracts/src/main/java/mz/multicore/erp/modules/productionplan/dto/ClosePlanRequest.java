package mz.multicore.erp.modules.productionplan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ClosePlanRequest(@NotNull @Positive Long companyId, @NotNull @Valid List<LeftoverRequest> leftovers) {}
