package com.phcpro.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateWorkSheetRequest(
    @NotNull(message = "O ID do ticket é obrigatório.")
    Long ticketId,

    @NotBlank(message = "O nome do técnico é obrigatório.")
    String technicianName,

    @NotNull(message = "As horas de trabalho são obrigatórias.")
    @PositiveOrZero(message = "As horas de trabalho não podem ser negativas.")
    BigDecimal hoursWorked,

    @NotBlank(message = "A descrição do trabalho é obrigatória.")
    @Size(max = 1000, message = "A descrição do trabalho não pode exceder 1000 caracteres.")
    String description,

    String partsUsed,

    @PositiveOrZero(message = "O custo das peças não pode ser negativo.")
    BigDecimal partsCost
) {}
