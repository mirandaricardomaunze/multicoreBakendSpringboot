package mz.multicore.erp.modules.comercial.dto;

import mz.multicore.erp.modules.comercial.model.CreditRiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Análise consolidada de risco de crédito por cliente, conjugando dívida em aberto,
 * limites acordados, maturidade da dívida e situação de bloqueio.
 */
public record ClientCreditRiskDTO(
        Long clientId,
        String clientName,
        String clientTaxId,
        String email,
        String address,
        BigDecimal creditLimit,
        BigDecimal totalDebt,
        BigDecimal availableCredit,
        LocalDate oldestDueDate,
        int maxDaysOverdue,
        BigDecimal corrente,
        BigDecimal ate30,
        BigDecimal de31a60,
        BigDecimal de61a90,
        BigDecimal maisDe90,
        BigDecimal totalOverdue,
        CreditRiskLevel riskLevel,
        boolean isBlocked,
        String blockReason
) {}
