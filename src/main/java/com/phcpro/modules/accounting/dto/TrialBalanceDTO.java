package com.phcpro.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Balancete de um período.
 *
 * @param balanced   os totais de débito e crédito coincidem — se não coincidirem há lançamentos
 *                   corrompidos e o balancete diz-o em vez de fingir que está bem
 */
public record TrialBalanceDTO(
        LocalDate from,
        LocalDate to,
        List<TrialBalanceLineDTO> lines,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced
) {}
