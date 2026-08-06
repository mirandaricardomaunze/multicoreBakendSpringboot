package com.phcpro.modules.purchases.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
        String serialNumber,
        /**
         * Taxa de IVA da linha <b>como vem na factura do fornecedor</b> (ex.: 0.16, 0.05, 0.00).
         * Ao contrário da venda, numa compra quem manda é o documento do fornecedor. Opcional:
         * vazio ⇒ aplica-se a taxa do artigo. Ver docs/IVA_TAXA_CANONICA_SPEC.md §4.
         */
        @DecimalMin(value = "0.00", message = "Taxa de IVA não pode ser negativa.")
        @DecimalMax(value = "1.00", message = "Taxa de IVA é uma fracção (ex.: 0.16 para 16%).")
        BigDecimal taxRate
) {
    /** Retrocompatível: chamadas antigas sem taxa continuam a compilar e caem na taxa do artigo. */
    public CreatePurchaseLineRequest(Long productId, BigDecimal quantity, BigDecimal unitPrice,
                                     String batchNumber, LocalDate expirationDate, String serialNumber) {
        this(productId, quantity, unitPrice, batchNumber, expirationDate, serialNumber, null);
    }
}
