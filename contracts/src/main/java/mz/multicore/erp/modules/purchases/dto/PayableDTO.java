package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Conta a pagar a fornecedor: fatura de compra com saldo em dívida. Total, pago e dívida estão na moeda do
 * documento; {@code baseTotalAmount} e {@code baseOutstanding} são o equivalente em moeda base, ao câmbio do
 * documento (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3).
 */
public record PayableDTO(
        Long purchaseId,
        String purchaseNumber,
        Long supplierId,
        String supplierName,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal outstanding,
        LocalDateTime purchaseDate,
        Long businessUnitId,
        String businessUnitName,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal baseTotalAmount,
        BigDecimal baseOutstanding
) {
    public PayableDTO(
            Long purchaseId,
            String purchaseNumber,
            Long supplierId,
            String supplierName,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            BigDecimal outstanding,
            LocalDateTime purchaseDate,
            Long businessUnitId,
            String businessUnitName
    ) {
        this(purchaseId, purchaseNumber, supplierId, supplierName, totalAmount, amountPaid, outstanding, purchaseDate,
                businessUnitId, businessUnitName, null, null, totalAmount, outstanding);
    }

    public PayableDTO(
            Long purchaseId,
            String purchaseNumber,
            Long supplierId,
            String supplierName,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            BigDecimal outstanding,
            LocalDateTime purchaseDate
    ) {
        this(purchaseId, purchaseNumber, supplierId, supplierName, totalAmount, amountPaid, outstanding, purchaseDate, null, null);
    }
}
