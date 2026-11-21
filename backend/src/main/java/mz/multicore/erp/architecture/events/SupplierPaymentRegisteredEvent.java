package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pagamento posterior de uma dívida a fornecedor, em moeda base (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.4).
 *
 * @param amount         o que saiu da tesouraria = valor × câmbio do pagamento
 * @param supplierAmount o que a dívida ao fornecedor baixou = valor ao câmbio do documento (na liquidação, o saldo
 *                       em moeda base que restava); igual a {@code amount} numa compra na moeda base
 */
public record SupplierPaymentRegisteredEvent(
        Long companyId,
        Long transactionId,
        String purchaseNumber,
        LocalDate date,
        BigDecimal amount,
        boolean cashPayment,
        BigDecimal supplierAmount
) {
    /** Pagamento na moeda base: a tesouraria e a dívida movem o mesmo valor. */
    public SupplierPaymentRegisteredEvent(Long companyId, Long transactionId, String purchaseNumber, LocalDate date,
                                          BigDecimal amount, boolean cashPayment) {
        this(companyId, transactionId, purchaseNumber, date, amount, cashPayment, amount);
    }
}
