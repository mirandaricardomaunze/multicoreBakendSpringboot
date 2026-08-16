package mz.multicore.erp.modules.accounting.dto;

import mz.multicore.erp.modules.accounting.model.AccountNature;

import java.math.BigDecimal;

/**
 * Linha do balancete: uma conta movimentada no período.
 *
 * @param balance saldo com o sinal certo para a natureza da conta (ver {@code AccountNature})
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
