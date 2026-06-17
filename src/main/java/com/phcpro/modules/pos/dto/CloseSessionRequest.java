package com.phcpro.modules.pos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CloseSessionRequest(
        @NotNull(message = "Saldo real de fecho é obrigatório.")
        @PositiveOrZero(message = "Saldo real não pode ser negativo.") BigDecimal closingBalanceReal,

        // Opcional: conta de tesouraria que recebe o depósito do numerário da sessão.
        // Se null, a sessão fecha sem gerar o depósito automático.
        Long depositAccountId
) {}
