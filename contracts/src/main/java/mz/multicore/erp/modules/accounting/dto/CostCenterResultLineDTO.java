package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;

/**
 * One cost centre of the "Resultados por centro de custo" report (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.3).
 *
 * @param costCenterId {@code null} only for the "Sem centro" row (class 6/7 lines posted before cost centres)
 * @param revenue      class 7, credit − debit
 * @param expense      class 6, debit − credit
 * @param result       revenue − expense
 */
public record CostCenterResultLineDTO(
        Long costCenterId,
        String costCenterCode,
        String costCenterName,
        BigDecimal revenue,
        BigDecimal expense,
        BigDecimal result
) {}
