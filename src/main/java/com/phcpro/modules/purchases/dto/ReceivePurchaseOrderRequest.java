package com.phcpro.modules.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/** Pedido de recepção parcial: quantidades a receber agora por linha da encomenda. */
public record ReceivePurchaseOrderRequest(
        @NotEmpty(message = "Indique pelo menos uma linha a receber.")
        @Valid List<ReceiveLine> lines
) {
    public record ReceiveLine(
            @NotNull(message = "Linha em falta.") Long lineId,
            @NotNull @Positive(message = "Quantidade a receber tem de ser positiva.") BigDecimal quantity
    ) {}
}
