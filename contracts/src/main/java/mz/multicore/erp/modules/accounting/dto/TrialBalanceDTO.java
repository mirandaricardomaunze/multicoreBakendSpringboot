package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Trial balance for a period.
 *
 * @param balanced   whether debit and credit totals match; a mismatch exposes corrupted entries
 */
public record TrialBalanceDTO(
        LocalDate from,
        LocalDate to,
        List<TrialBalanceLineDTO> lines,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced
) {}
