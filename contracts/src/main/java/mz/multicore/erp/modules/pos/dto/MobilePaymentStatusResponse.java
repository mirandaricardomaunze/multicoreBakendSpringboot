package mz.multicore.erp.modules.pos.dto;

/**
 * Resposta de consulta periódica (polling) de status do pagamento móvel.
 */
public record MobilePaymentStatusResponse(
        String transactionId,
        MobilePaymentStatus status,
        String financialReference,
        String message
) {}
