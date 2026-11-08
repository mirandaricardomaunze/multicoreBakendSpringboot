package mz.multicore.erp.modules.financeira.dto;

import mz.multicore.erp.modules.financeira.model.BankStatementItemStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bank-statement transaction stored in the database with its reconciliation status.
 */
public record BankStatementItemDTO(
        Long id,
        Long statementId,
        LocalDate transactionDate,
        LocalDate valueDate,
        String description,
        String reference,
        BigDecimal amount,
        BigDecimal balanceAfter,
        BankStatementItemStatus status,
        Long matchedTransactionId,
        String notes
) {}
