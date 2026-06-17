package com.phcpro.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateInvoiceLineRequest(
    @NotNull(message = "O ID do produto é obrigatório.")
    Long productId,

    @NotNull(message = "A quantidade é obrigatória.")
    @Positive(message = "A quantidade deve ser positiva.")
    BigDecimal quantity,

    @NotNull(message = "A taxa de imposto é obrigatória.")
    BigDecimal taxRate, // e.g. 0.23

    BigDecimal discountPercentage, // e.g. 10.00
    String batchNumber,            // Lote
    String serialNumber            // Série
) {
    public CreateInvoiceLineRequest(Long productId, Integer quantity, BigDecimal taxRate) {
        this(productId, BigDecimal.valueOf(quantity), taxRate, BigDecimal.ZERO, null, null);
    }

    public CreateInvoiceLineRequest(Long productId, Integer quantity, BigDecimal taxRate,
                                    BigDecimal discountPercentage, String batchNumber, String serialNumber) {
        this(productId, BigDecimal.valueOf(quantity), taxRate, discountPercentage, batchNumber, serialNumber);
    }
}
