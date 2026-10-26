package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação imutável de uma prestação / parcela fracionada de fatura.
 */
public record InvoiceInstallmentDTO(
        Long id,
        Long invoiceId,
        int installmentNumber,
        int totalInstallments,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal remainingBalance,
        String status,
        String notes
) {
    public InvoiceInstallmentDTO {
        if (amount == null) amount = BigDecimal.ZERO;
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        if (remainingBalance == null) remainingBalance = amount.subtract(paidAmount);
    }
}
