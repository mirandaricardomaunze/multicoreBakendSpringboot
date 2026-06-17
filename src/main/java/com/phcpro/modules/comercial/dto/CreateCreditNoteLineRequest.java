package com.phcpro.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Linha de uma nota de crédito. Aponta para uma {@code InvoiceLine} concreta —
 * produto, lote, preço e IVA são herdados dessa linha. O operador escolhe apenas
 * <b>quanto</b> devolver (limitado pela quantidade vendida menos o já devolvido).
 */
public record CreateCreditNoteLineRequest(
        @NotNull Long invoiceLineId,
        @NotNull @Positive BigDecimal quantity
) {}
