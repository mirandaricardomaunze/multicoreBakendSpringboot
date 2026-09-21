package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Facto contabilistico emitido quando uma fatura de compra entra no sistema. */
public record PurchaseRegisteredEvent(
        Long companyId,
        Long purchaseId,
        String purchaseNumber,
        LocalDate date,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaidNow,
        boolean cashPayment
) {}
