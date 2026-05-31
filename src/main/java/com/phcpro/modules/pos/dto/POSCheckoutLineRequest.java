package com.phcpro.modules.pos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record POSCheckoutLineRequest(
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @NotNull(message = "Quantidade é obrigatória.")
        @PositiveOrZero(message = "Quantidade não pode ser negativa.") Integer quantity,
        @PositiveOrZero(message = "Desconto não pode ser negativo.") BigDecimal discountPercentage,
        String batchNumber,
        String serialNumber
) {}
