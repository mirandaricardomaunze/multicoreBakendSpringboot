package com.phcpro.modules.pos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CloseSessionRequest(
        @NotNull(message = "Saldo real de fecho é obrigatório.")
        @PositiveOrZero(message = "Saldo real não pode ser negativo.") BigDecimal closingBalanceReal
) {}
