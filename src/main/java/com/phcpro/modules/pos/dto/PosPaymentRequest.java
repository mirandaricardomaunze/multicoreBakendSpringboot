package com.phcpro.modules.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Uma linha de pagamento submetida no checkout.
 * - {@code method}: CASH / CARD / BANK_TRANSFER / CREDIT
 * - {@code amount}: valor que esta entrada cobre
 * - {@code tenderedAmount}: para CASH, o valor que o cliente entregou (≥ amount); o troco é calculado pelo service
 * - {@code reference}: nº de autorização do cartão ou comprovativo de transferência
 * - {@code treasuryAccountId}: conta de tesouraria a movimentar (obrigatório para CASH/CARD/BANK_TRANSFER; null para CREDIT)
 */
public record PosPaymentRequest(
        @NotBlank String method,
        @NotNull @Positive BigDecimal amount,
        @PositiveOrZero BigDecimal tenderedAmount,
        String reference,
        Long treasuryAccountId
) {}
