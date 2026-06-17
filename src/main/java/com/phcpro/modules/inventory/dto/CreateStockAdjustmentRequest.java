package com.phcpro.modules.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateStockAdjustmentRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        @NotNull(message = "Quantidade contada é obrigatória.")
        @PositiveOrZero(message = "Quantidade contada não pode ser negativa.") BigDecimal countedQuantity,
        @NotBlank(message = "Motivo do ajuste é obrigatório.") String reason
) {}
