package com.phcpro.modules.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateExpenseClaimRequest(
    @NotNull(message = "O ID do colaborador é obrigatório.")
    Long employeeId,

    @NotNull(message = "O valor da despesa é obrigatório.")
    @Positive(message = "O valor da despesa deve ser superior a zero.")
    BigDecimal amount,

    @NotBlank(message = "A categoria é obrigatória.")
    String category,

    @NotBlank(message = "A descrição é obrigatória.")
    @Size(max = 500, message = "A descrição não pode exceder 500 caracteres.")
    String description
) {}
