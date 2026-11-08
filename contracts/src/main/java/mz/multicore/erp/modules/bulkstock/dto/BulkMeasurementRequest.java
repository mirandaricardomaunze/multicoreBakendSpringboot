package mz.multicore.erp.modules.bulkstock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * A pile measurement. For {@code DIRECT} send {@code quantity} in the product base unit; for shape methods
 * send the dimensions in metres and the server converts the volume to the base unit.
 */
public record BulkMeasurementRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        @NotBlank(message = "Indique o método de medição.") String method,
        BigDecimal quantity,
        BigDecimal dimension1,
        BigDecimal dimension2,
        BigDecimal dimension3,
        String notes
) {}
