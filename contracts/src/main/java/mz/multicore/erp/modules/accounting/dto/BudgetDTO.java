package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Annual budget of a company (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.1).
 *
 * @param id            {@code null} while the year has no budget saved yet
 * @param totalExpenses sum of the class 6 cells
 * @param totalRevenues sum of the class 7 cells
 */
public record BudgetDTO(
        Long id,
        int year,
        List<BudgetLineDTO> lines,
        BigDecimal totalExpenses,
        BigDecimal totalRevenues
) {}
