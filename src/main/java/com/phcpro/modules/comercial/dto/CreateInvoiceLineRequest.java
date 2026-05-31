package com.phcpro.modules.comercial.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateInvoiceLineRequest(
    @NotNull(message = "O ID do produto é obrigatório.")
    Long productId,

    @NotNull(message = "A quantidade é obrigatória.")
    @Min(value = 1, message = "A quantidade mínima é 1.")
    Integer quantity,

    @NotNull(message = "A taxa de imposto é obrigatória.")
    BigDecimal taxRate, // e.g. 0.23

    BigDecimal discountPercentage, // e.g. 10.00
    String batchNumber,            // Lote
    String serialNumber            // Série
) {
    public CreateInvoiceLineRequest(Long productId, Integer quantity, BigDecimal taxRate) {
        this(productId, quantity, taxRate, BigDecimal.ZERO, null, null);
    }
}
