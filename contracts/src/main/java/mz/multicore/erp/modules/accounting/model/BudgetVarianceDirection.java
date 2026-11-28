package mz.multicore.erp.modules.accounting.model;

/**
 * Sense of a budget variance (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.2, OR-04): a cost above budget or a revenue
 * below budget is unfavourable; the inverse is favourable; no variance is on budget.
 */
public enum BudgetVarianceDirection {
    FAVORABLE,
    UNFAVORABLE,
    ON_BUDGET
}
