package com.phcpro.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Extracto de uma conta (razão) num período.
 *
 * @param openingBalance saldo à data inicial (tudo o que foi lançado antes)
 * @param closingBalance saldo no fim do período
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
