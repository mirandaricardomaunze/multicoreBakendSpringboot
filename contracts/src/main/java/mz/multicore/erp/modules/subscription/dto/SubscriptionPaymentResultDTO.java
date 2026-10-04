package mz.multicore.erp.modules.subscription.dto;

/**
 * Resultado da operação de pagamento ou ativação de plano.
 */
public record SubscriptionPaymentResultDTO(
        boolean success,
        String message,
        String transactionId,
        MySubscriptionDTO subscription
) {}
