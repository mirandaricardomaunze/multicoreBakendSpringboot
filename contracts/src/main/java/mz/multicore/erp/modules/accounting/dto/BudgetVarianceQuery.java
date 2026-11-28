package mz.multicore.erp.modules.accounting.dto;

/**
 * Parameters of the real vs orçado report (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.2).
 *
 * @param month        1–12
 * @param cumulative   {@code true} sums January up to {@code month}; {@code false} reports only {@code month}
 * @param costCenterId optional centre filter; {@code null} reports every centre
 */
public record BudgetVarianceQuery(
        Integer year,
        Integer month,
        boolean cumulative,
        Long costCenterId
) {}
