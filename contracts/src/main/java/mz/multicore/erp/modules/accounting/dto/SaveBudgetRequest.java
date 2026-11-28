package mz.multicore.erp.modules.accounting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Saves the annual budget of the active company (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.1, OR-01): the request
 * carries the whole year, so cells left out are removed and cells sent are created or updated.
 *
 * @param year  fiscal year of the budget
 * @param lines one cell per account × cost centre × month
 */
public record SaveBudgetRequest(
        @NotNull(message = "Indique o ano do orçamento.")
        Integer year,

        @Valid
        List<Line> lines
) {

    /**
     * One budget cell: a postable account of class 6 or 7, an active cost centre, a month (1–12) and an amount ≥ 0.
     */
    public record Line(
            String accountCode,
            Long costCenterId,
            Integer month,
            BigDecimal amount
    ) {}
}
