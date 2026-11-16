package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money left the treasury automatically (HR payments, reimbursed expenses). Without this the cash left the treasury
 * but not the books: posted debit by {@code purpose} / credit Caixa or Banco.
 */
public record TreasuryPayoutEvent(Long companyId, Long transactionId, LocalDate date, BigDecimal amount,
                                  PayoutPurpose purpose, boolean cash, String description) {}
