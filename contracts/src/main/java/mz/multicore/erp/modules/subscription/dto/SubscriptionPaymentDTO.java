package mz.multicore.erp.modules.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionPaymentDTO(
        Long id,
        BigDecimal amount,
        String method,
        String methodLabel,
        LocalDate paidAt,
        LocalDate periodStart,
        LocalDate periodEnd,
        String note
) {}
