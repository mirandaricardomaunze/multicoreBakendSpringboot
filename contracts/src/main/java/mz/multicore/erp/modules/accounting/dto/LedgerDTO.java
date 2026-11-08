package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Account ledger statement for a period.
 *
 * @param openingBalance balance at the start date, including all prior entries
 * @param closingBalance balance at the end of the period
 */
public record LedgerDTO(
        String accountCode,
        String accountName,
        LocalDate from,
        LocalDate to,
        BigDecimal openingBalance,
        List<LedgerMovementDTO> movements,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal closingBalance
) {}
