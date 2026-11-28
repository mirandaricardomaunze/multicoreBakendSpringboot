package mz.multicore.erp.modules.accounting.dto;

import java.util.List;

/**
 * Real vs orçado report of the active company (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.2).
 *
 * @param cumulative   {@code true} when the period runs from January up to {@code month}
 * @param costCenterId centre the report was filtered by, {@code null} for all centres
 */
public record BudgetVarianceDTO(
        int year,
        int month,
        boolean cumulative,
        Long costCenterId,
        List<BudgetVarianceLineDTO> lines
) {}
