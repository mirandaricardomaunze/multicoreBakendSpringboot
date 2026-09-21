package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;

/**
 * Pedido de excepção comercial para desbloqueio de venda a cliente em risco de crédito.
 */
public record CreditExceptionApprovalRequest(
        Long clientId,
        BigDecimal requestedAmount,
        String reason,
        String salesperson
) {}
