package com.phcpro.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimento no razão de uma conta.
 *
 * @param runningBalance saldo acumulado até este movimento, inclusive
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
