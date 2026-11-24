package mz.multicore.erp.modules.accounting.dto;

import mz.multicore.erp.modules.accounting.model.BudgetVarianceDirection;

import java.math.BigDecimal;

/**
 * Budget against actual of one account × cost centre in the reported period (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md
 * §4.2). {@code actual} is signed by class: debit − credit for class 6, credit − debit for class 7.
 *
 * @param accountClass    class digit of the account (6 or 7)
 * @param variance        actual − budgeted
 * @param variancePercent variance ÷ budgeted × 100, 2 decimals; {@code null} when nothing was budgeted
 * @param direction       favourable / unfavourable by class, on budget when the variance is zero
 */
public record BudgetVarianceLineDTO(
        String accountCode,
        String accountName,
        int accountClass,
        Long costCenterId,
        String costCenterCode,
        String costCenterName,
        BigDecimal budgeted,
        BigDecimal actual,
        BigDecimal variance,
        BigDecimal variancePercent,
        BudgetVarianceDirection direction
) {}
