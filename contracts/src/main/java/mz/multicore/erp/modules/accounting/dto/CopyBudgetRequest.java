package mz.multicore.erp.modules.accounting.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Fills the empty cells of a year's budget with the actual of the previous year
 * (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.1, OR-02).
 *
 * @param year              target fiscal year (the actual comes from {@code year - 1})
 * @param adjustmentPercent optional adjustment, e.g. {@code 10} for +10%; {@code null} copies the actual as is
 */
public record CopyBudgetRequest(
        @NotNull(message = "Indique o ano do orçamento.")
        Integer year,

        BigDecimal adjustmentPercent
) {}
