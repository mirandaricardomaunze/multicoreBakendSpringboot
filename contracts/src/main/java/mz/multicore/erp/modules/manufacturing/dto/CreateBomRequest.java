package mz.multicore.erp.modules.manufacturing.dto;

import mz.multicore.erp.modules.manufacturing.model.BomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pedido de criação de nova ficha técnica (BOM).
 */
public record CreateBomRequest(
        @NotBlank(message = "O código da ficha técnica é obrigatório.")
        @Size(max = 32) String bomCode,

        @NotBlank(message = "A designação da ficha técnica é obrigatória.")
        @Size(max = 200) String name,

        @NotNull(message = "O produto acabado é obrigatório.")
        Long finishedProductId,

        @NotNull(message = "A quantidade base de produção é obrigatória.")
        @Positive(message = "A quantidade base deve ser positiva.")
        BigDecimal baseQuantity,

        BomType bomType,

        BigDecimal estimatedLaborCost,
        BigDecimal estimatedOverheadCost,

        @NotEmpty(message = "A ficha técnica deve conter pelo menos uma matéria-prima.")
        List<CreateBomItemRequest> items
) {
    public record CreateBomItemRequest(
            @NotNull Long rawMaterialProductId,
            @NotNull @Positive BigDecimal requiredQuantity,
            BigDecimal scrapPercentage
    ) {}
}
