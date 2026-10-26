package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumo consolidado do Radar de Cobranças por horizontes temporais.
 */
public record CollectionRadarSummaryDTO(
        Long companyId,
        LocalDate referenceDate,
        BigDecimal criticalOverdueTotal,
        int criticalOverdueCount,
        BigDecimal recentOverdueTotal,
        int recentOverdueCount,
        BigDecimal dueTodayTotal,
        int dueTodayCount,
        BigDecimal dueIn7DaysTotal,
        int dueIn7DaysCount,
        BigDecimal dueIn15DaysTotal,
        int dueIn15DaysCount,
        BigDecimal dueIn30DaysTotal,
        int dueIn30DaysCount,
        BigDecimal totalReceivable,
        int totalInvoicesCount
) {
    public CollectionRadarSummaryDTO {
        if (criticalOverdueTotal == null) criticalOverdueTotal = BigDecimal.ZERO;
        if (recentOverdueTotal == null) recentOverdueTotal = BigDecimal.ZERO;
        if (dueTodayTotal == null) dueTodayTotal = BigDecimal.ZERO;
        if (dueIn7DaysTotal == null) dueIn7DaysTotal = BigDecimal.ZERO;
        if (dueIn15DaysTotal == null) dueIn15DaysTotal = BigDecimal.ZERO;
        if (dueIn30DaysTotal == null) dueIn30DaysTotal = BigDecimal.ZERO;
        if (totalReceivable == null) totalReceivable = BigDecimal.ZERO;
    }
}
