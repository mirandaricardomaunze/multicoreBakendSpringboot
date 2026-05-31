package com.phcpro.modules.purchases.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePurchaseLineRequest(
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @NotNull(message = "Quantidade é obrigatória.")
        @Positive(message = "Quantidade deve ser positiva.") BigDecimal quantity,
        @NotNull(message = "Preço unitário é obrigatório.")
        @Positive(message = "Preço unitário deve ser positivo.") BigDecimal unitPrice,
        String batchNumber,
        @NotNull(message = "Validade do lote é obrigatória.") LocalDate expirationDate,
        String serialNumber
) {}
