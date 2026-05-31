package com.phcpro.modules.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record OpenSessionRequest(
        @NotBlank(message = "Operador é obrigatório.") String operator,
        @NotNull(message = "Saldo inicial é obrigatório.")
        @PositiveOrZero(message = "Saldo inicial não pode ser negativo.") BigDecimal openingBalance,
        @NotNull(message = "Empresa é obrigatória.") Long companyId
) {}
