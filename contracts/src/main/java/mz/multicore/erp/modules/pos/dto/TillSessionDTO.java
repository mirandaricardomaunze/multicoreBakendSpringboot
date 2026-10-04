package mz.multicore.erp.modules.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TillSessionDTO(
        Long id,
        String operator,
        String currentOperator,
        Long companyId,
        BigDecimal openingBalance,
        BigDecimal closingBalanceExpected,
        BigDecimal closingBalanceReal,
        BigDecimal difference,
        LocalDateTime openDate,
        LocalDateTime closeDate,
        String status,
        String closingNotes,
        String cashBreakdownJson
) {
    /** Retrocompatível: sem currentOperator, closingNotes nem cashBreakdownJson. */
    public TillSessionDTO(
            Long id,
            String operator,
            Long companyId,
            BigDecimal openingBalance,
            BigDecimal closingBalanceExpected,
            BigDecimal closingBalanceReal,
            BigDecimal difference,
            LocalDateTime openDate,
            LocalDateTime closeDate,
            String status
    ) {
        this(id, operator, null, companyId, openingBalance, closingBalanceExpected, closingBalanceReal, difference, openDate, closeDate, status, null, null);
    }

    /** Retrocompatível: sem currentOperator. */
    public TillSessionDTO(
            Long id,
            String operator,
            Long companyId,
            BigDecimal openingBalance,
            BigDecimal closingBalanceExpected,
            BigDecimal closingBalanceReal,
            BigDecimal difference,
            LocalDateTime openDate,
            LocalDateTime closeDate,
            String status,
            String closingNotes,
            String cashBreakdownJson
    ) {
        this(id, operator, null, companyId, openingBalance, closingBalanceExpected, closingBalanceReal, difference, openDate, closeDate, status, closingNotes, cashBreakdownJson);
    }
}
