package mz.multicore.erp.modules.assets.dto;

import mz.multicore.erp.modules.assets.model.AssetCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pedido de registo de novo activo fixo.
 */
public record CreateAssetRequest(
        @NotBlank(message = "O código do activo é obrigatório.")
        @Size(max = 32, message = "O código do activo não pode exceder 32 caracteres.")
        String assetCode,

        @NotBlank(message = "O nome/descrição do activo é obrigatório.")
        @Size(max = 200, message = "A descrição não pode exceder 200 caracteres.")
        String name,

        @NotNull(message = "A categoria do activo é obrigatória.")
        AssetCategory category,

        @NotNull(message = "A data de aquisição é obrigatória.")
        LocalDate acquisitionDate,

        @NotNull(message = "O custo de aquisição é obrigatório.")
        @Positive(message = "O custo de aquisição deve ser positivo.")
        BigDecimal acquisitionCost,

        BigDecimal residualValue,

        BigDecimal annualDepreciationRate,

        String serialNumber,
        String location,
        String assignedEmployee
) {}
