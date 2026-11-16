package mz.multicore.erp.modules.accounting.dto;

import mz.multicore.erp.modules.accounting.model.AccountNature;

import java.math.BigDecimal;

/**
 * Trial-balance line for an account with movements during the period.
 *
 * @param balance balance with the sign appropriate to the account nature (see {@code AccountNature})
 */
public record TrialBalanceLineDTO(
        String accountCode,
        String accountName,
        String classLabel,
        AccountNature nature,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal balance
) {}
