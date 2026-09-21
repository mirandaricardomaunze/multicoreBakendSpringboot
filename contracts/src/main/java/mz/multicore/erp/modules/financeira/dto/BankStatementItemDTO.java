package mz.multicore.erp.modules.financeira.dto;

import mz.multicore.erp.modules.financeira.model.BankStatementItemStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimento do extracto bancário já registado na base de dados com o respetivo estado de conciliação.
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
