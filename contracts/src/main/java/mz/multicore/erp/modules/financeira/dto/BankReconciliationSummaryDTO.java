package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Indicadores consolidados de reconciliação de uma conta bancária.
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
