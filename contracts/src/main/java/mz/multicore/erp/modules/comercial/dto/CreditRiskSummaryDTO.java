package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumo executivo dos indicadores globais de risco de crédito da carteira.
 */
public record CreditRiskSummaryDTO(
        LocalDate referenceDate,
        BigDecimal totalReceivable,
        BigDecimal totalOverdue,
        BigDecimal totalCreditLimit,
        int totalDebtorClients,
        int blockedClientsCount,
        int criticalRiskCount,
        int highRiskCount,
        int mediumRiskCount,
        int lowRiskCount
) {}
