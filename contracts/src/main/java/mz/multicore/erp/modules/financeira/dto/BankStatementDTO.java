package mz.multicore.erp.modules.financeira.dto;

import mz.multicore.erp.modules.financeira.model.BankReconciliationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resumo do extracto bancário e progresso da conciliação.
 */
public record BankStatementDTO(
        Long id,
        Long treasuryAccountId,
        String accountName,
        String statementReference,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal calculatedBalance,
        BigDecimal difference,
        BankReconciliationStatus status,
        int totalItems,
        int matchedItemsCount,
        int unmatchedItemsCount,
        LocalDateTime createdAt
) {}
