package com.phcpro.modules.pos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record POSCheckoutLineRequest(
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @NotNull(message = "Quantidade é obrigatória.")
        @Positive(message = "Quantidade deve ser positiva.") BigDecimal quantity,
        @PositiveOrZero(message = "Desconto não pode ser negativo.") BigDecimal discountPercentage,
        String batchNumber,
        String serialNumber
) {
    public POSCheckoutLineRequest(Long productId, Integer quantity, BigDecimal discountPercentage,
                                  String batchNumber, String serialNumber) {
        this(productId, BigDecimal.valueOf(quantity), discountPercentage, batchNumber, serialNumber);
    }
}
