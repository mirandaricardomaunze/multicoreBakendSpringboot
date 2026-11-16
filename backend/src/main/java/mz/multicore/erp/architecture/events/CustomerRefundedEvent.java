package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money went back to a customer for a return at the POS (cash from the drawer, or card/bank/M-Pesa). The credit note
 * already reduced the sale and credited the customer; this posts the money leaving: debit Clientes, credit Caixa or
 * Banco. A refund as a store voucher or on account moves no money and publishes nothing.
 *
 * @param creditNoteId the credit note of the return, so a retry never posts twice
 * @param cash         the money left the drawer (Caixa) rather than a bank account
 */
public record CustomerRefundedEvent(Long companyId, Long creditNoteId, String noteNumber, LocalDate date,
                                    BigDecimal amount, boolean cash) {}
