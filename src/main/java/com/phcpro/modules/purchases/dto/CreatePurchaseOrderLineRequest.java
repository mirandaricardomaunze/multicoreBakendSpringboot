package com.phcpro.modules.purchases.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePurchaseOrderLineRequest(
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @NotNull(message = "Quantidade é obrigatória.")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser positiva.") BigDecimal quantity,
        @NotNull(message = "Preço unitário é obrigatório.")
        @DecimalMin(value = "0.00", message = "Preço não pode ser negativo.") BigDecimal unitPrice,
        String batchNumber,
        LocalDate expirationDate,
        String serialNumber
) {}
