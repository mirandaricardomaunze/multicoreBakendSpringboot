package mz.multicore.erp.modules.pos.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resposta de iniciação de pagamento móvel.
 */
public record MobilePaymentResponse(
        String transactionId,
        MobilePaymentProvider provider,
        String phoneNumber,
        BigDecimal amount,
        String reference,
        String financialReference,
        MobilePaymentStatus status,
        String message,
        Instant createdAt
) {}
