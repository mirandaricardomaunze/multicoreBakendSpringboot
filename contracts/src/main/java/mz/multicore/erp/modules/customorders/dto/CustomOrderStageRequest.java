package mz.multicore.erp.modules.customorders.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import mz.multicore.erp.modules.customorders.model.CustomOrderStage;

public record CustomOrderStageRequest(@NotNull @Positive Long companyId, @NotNull CustomOrderStage stage) {}
