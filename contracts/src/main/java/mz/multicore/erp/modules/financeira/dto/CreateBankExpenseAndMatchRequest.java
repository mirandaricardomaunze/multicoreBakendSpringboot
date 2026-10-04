package mz.multicore.erp.modules.financeira.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Pedido para registar um encargo ou comissão bancária diretamente a partir do extracto e reconciliar de imediato.
 *
 * <p>{@code amount} nulo é válido: o serviço assume o valor absoluto da linha do extracto.
 */
public record CreateBankExpenseAndMatchRequest(
        @NotNull(message = "A linha do extracto é obrigatória.")
        Long statementItemId,

        @Size(max = 255, message = "A descrição não pode exceder 255 caracteres.")
        String description,

        @Positive(message = "O valor do encargo bancário deve ser positivo.")
        BigDecimal amount
) {}
