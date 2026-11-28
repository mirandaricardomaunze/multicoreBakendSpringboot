package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * "Resultados por centro de custo" for a period (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.3): one line per centre with
 * movements, ordered by code, and the "Total" columns, equal to the income statement of the same period.
 */
public record CostCenterResultDTO(
        LocalDate from,
        LocalDate to,
        List<CostCenterResultLineDTO> centers,
        BigDecimal totalRevenue,
        BigDecimal totalExpense,
        BigDecimal netResult
) {}
