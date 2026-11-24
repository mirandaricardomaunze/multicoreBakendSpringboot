package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;

/**
 * Budget against actual of one account × cost centre in the reported period (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md
 * §4.2). {@code actual} is signed by class: debit − credit for class 6, credit − debit for class 7.
 *
 * @param accountClass class digit of the account (6 or 7)
 */
public record BudgetVarianceLineDTO(
        String accountCode,
        String accountName,
        int accountClass,
        Long costCenterId,
        String costCenterCode,
        String costCenterName,
        BigDecimal budgeted,
        BigDecimal actual
) {}
