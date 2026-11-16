package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The supplier paid back money it owed after a return of goods already paid for: debit Caixa or Banco / credit
 * Fornecedores, clearing the debit balance the return left on the supplier.
 */
public record SupplierRefundReceivedEvent(Long companyId, Long transactionId, String purchaseNumber, LocalDate date,
                                          BigDecimal amount, boolean cash) {}
