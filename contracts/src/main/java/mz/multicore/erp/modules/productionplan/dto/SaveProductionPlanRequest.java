package mz.multicore.erp.modules.productionplan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Planned quantities of the day. Lines already produced keep their production; a line can only grow or shrink. */
public record SaveProductionPlanRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive Long warehouseId,
        @NotNull LocalDate planDate,
        @Size(max = 300) String notes,
        @NotNull @Valid List<PlanLineRequest> lines
) {}
