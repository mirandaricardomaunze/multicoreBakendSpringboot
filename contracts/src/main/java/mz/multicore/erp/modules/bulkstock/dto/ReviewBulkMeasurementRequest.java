package mz.multicore.erp.modules.bulkstock.dto;

import jakarta.validation.constraints.NotNull;

/** Manager decision on a measurement above tolerance; a reason is required to reject. */
public record ReviewBulkMeasurementRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        String reason
) {}
