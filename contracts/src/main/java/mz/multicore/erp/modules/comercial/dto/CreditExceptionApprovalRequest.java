package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Pedido de excepção comercial para desbloqueio de venda a cliente em risco de crédito.
 *
 * <p>O motivo mínimo (10 caracteres) é regra de negócio do {@code CreditRiskService}.
 */
public record CreditExceptionApprovalRequest(
        @NotNull(message = "O cliente é obrigatório.")
        Long clientId,

        @PositiveOrZero(message = "O valor pedido não pode ser negativo.")
        BigDecimal requestedAmount,

        @Size(max = 500, message = "O motivo não pode exceder 500 caracteres.")
        String reason,

        @Size(max = 100, message = "O nome do vendedor não pode exceder 100 caracteres.")
        String salesperson
) {}
