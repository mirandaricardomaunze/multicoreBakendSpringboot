package mz.multicore.erp.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Correcção de uma folha de obra ainda <b>por faturar</b>. Não muda o pedido associado: se a folha
 * foi lançada no pedido errado, anula-se e regista-se outra.
 */
public record UpdateWorkSheetRequest(
    @NotBlank(message = "O nome do técnico é obrigatório.")
    String technicianName,

    @NotNull(message = "As horas executadas são obrigatórias.")
    @PositiveOrZero(message = "As horas não podem ser negativas.")
    BigDecimal hoursWorked,

    @NotBlank(message = "A descrição do serviço é obrigatória.")
    @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres.")
    String description,

    @Size(max = 500, message = "As peças não podem exceder 500 caracteres.")
    String partsUsed,

    @PositiveOrZero(message = "O custo das peças não pode ser negativo.")
    BigDecimal partsCost
) {}
