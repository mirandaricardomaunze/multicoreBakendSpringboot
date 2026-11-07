package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Quantidade conferida de uma linha. Zero é válido: o artigo esgotou e não saiu — a conferência
 * tem de o dizer em vez de omitir a linha.
 */
public record OrderPickingConfirmationLineRequest(
    @NotNull @Positive Long lineId,
    @NotNull @PositiveOrZero BigDecimal confirmedQuantity,
    @Size(max = 120) String batchNumber
) {}
