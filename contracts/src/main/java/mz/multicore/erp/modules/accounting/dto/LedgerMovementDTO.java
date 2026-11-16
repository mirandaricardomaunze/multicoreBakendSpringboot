package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movement in an account ledger.
 *
 * @param runningBalance cumulative balance through this movement
 */
public record LedgerMovementDTO(
        LocalDate date,
        String entryNumber,
        String description,
        String sourceDocumentNumber,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal runningBalance
) {}
