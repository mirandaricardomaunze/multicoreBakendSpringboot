package com.phcpro.modules.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CashMovementRequest(
        @NotBlank(message = "Tipo de movimento é obrigatório.")
        @Pattern(regexp = "SUPRIMENTO|SANGRIA", message = "Tipo deve ser SUPRIMENTO ou SANGRIA.") String type,
        @NotNull(message = "Valor é obrigatório.")
        @Positive(message = "Valor deve ser positivo.") BigDecimal amount,
        String description
) {}
