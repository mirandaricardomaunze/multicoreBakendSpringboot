package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Consolidated reconciliation indicators for a bank account.
 */
public record BankReconciliationSummaryDTO(
        Long treasuryAccountId,
        String accountName,
        BigDecimal bankBalance,
        BigDecimal systemBalance,
        BigDecimal difference,
        int totalPendingItems,
        int openStatementsCount,
        LocalDate lastReconciledDate
) {}
