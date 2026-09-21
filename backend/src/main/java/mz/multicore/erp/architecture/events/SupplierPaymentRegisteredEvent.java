package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pagamento posterior de uma dívida a fornecedor. */
public record SupplierPaymentRegisteredEvent(
        Long companyId,
        Long transactionId,
        String purchaseNumber,
        LocalDate date,
        BigDecimal amount,
        boolean cashPayment
) {}
