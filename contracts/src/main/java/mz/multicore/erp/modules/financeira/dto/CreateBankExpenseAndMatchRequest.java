package mz.multicore.erp.modules.financeira.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request to record a bank charge or fee directly from a statement and reconcile it immediately.
 *
 * <p>A null {@code amount} is valid; the service uses the absolute value of the statement line.
 */
public record CreateBankExpenseAndMatchRequest(
        @NotNull(message = "A linha do extracto é obrigatória.")
        Long statementItemId,

        @Size(max = 255, message = "A descrição não pode exceder 255 caracteres.")
        String description,

        @Positive(message = "O valor do encargo bancário deve ser positivo.")
        BigDecimal amount
) {}
