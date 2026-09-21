package mz.multicore.erp.modules.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registo manual de um pagamento de assinatura. O período coberto estende a validade. */
public record RecordPaymentRequest(
        @NotNull BigDecimal amount,
        @NotBlank String method,
        LocalDate paidAt,
        LocalDate periodStart,
        LocalDate periodEnd,
        String reference,
        String paymentDetails,
        String note
) {
    public RecordPaymentRequest(BigDecimal amount, String method, LocalDate paidAt, LocalDate periodStart, LocalDate periodEnd, String note) {
        this(amount, method, paidAt, periodStart, periodEnd, null, null, note);
    }
}
