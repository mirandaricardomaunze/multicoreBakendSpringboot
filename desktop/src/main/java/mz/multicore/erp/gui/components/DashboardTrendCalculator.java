package mz.multicore.erp.gui.components;

import mz.multicore.erp.gui.DashboardPanel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Motor determinístico de cálculo de tendências, períodos comparativos e métricas
 * analíticas executivas para o Dashboard Comercial e de Gestão.
 */
public final class DashboardTrendCalculator {

    private DashboardTrendCalculator() {
    }

    /**
     * Intervalo de datas fechado [from, to]. Valores nulos indicam limites abertos.
     */
    public record DateRange(LocalDate from, LocalDate to) {
        public boolean contains(LocalDate date) {
            if (date == null) return false;
            if (from != null && date.isBefore(from)) return false;
            if (to != null && date.isAfter(to)) return false;
            return true;
        }
    }

    /**
     * Par de períodos temporalmente alinhados (atual vs anterior) com rótulo descritivo.
     */
    public record ComparisonPeriod(DateRange current, DateRange previous, String comparisonLabel) {
    }

    /**
     * Resolve os intervalos de datas atual e anterior com base no filtro selecionado e na data âncora.
     */
    public static ComparisonPeriod resolvePeriods(DashboardPanel.PeriodFilter filter, LocalDate anchorDate) {
        LocalDate today = anchorDate != null ? anchorDate : LocalDate.now();
        DashboardPanel.PeriodFilter activeFilter = filter != null ? filter : DashboardPanel.PeriodFilter.HOJE;

        return switch (activeFilter) {
            case HOJE -> {
                DateRange current = new DateRange(today, today);
                DateRange prev = new DateRange(today.minusDays(1), today.minusDays(1));
                yield new ComparisonPeriod(current, prev, "vs ontem");
            }
            case ESTA_SEMANA -> {
                DateRange current = new DateRange(today.minusDays(7), today);
                DateRange prev = new DateRange(today.minusDays(14), today.minusDays(7));
                yield new ComparisonPeriod(current, prev, "vs semana anterior");
            }
            case ESTE_MES -> {
                LocalDate startCurrent = today.withDayOfMonth(1);
                LocalDate endPrev = startCurrent.minusDays(1);
                LocalDate startPrev = endPrev.withDayOfMonth(1);
                DateRange current = new DateRange(startCurrent, today);
                DateRange prev = new DateRange(startPrev, endPrev);
                yield new ComparisonPeriod(current, prev, "vs mês anterior");
            }
            case ESTE_ANO -> {
                LocalDate startCurrent = today.withDayOfYear(1);
                LocalDate endPrev = startCurrent.minusDays(1);
                LocalDate startPrev = endPrev.withDayOfYear(1);
                DateRange current = new DateRange(startCurrent, today);
                DateRange prev = new DateRange(startPrev, endPrev);
                yield new ComparisonPeriod(current, prev, "vs ano anterior");
            }
            case TODOS -> {
                DateRange current = new DateRange(null, null);
                yield new ComparisonPeriod(current, null, "histórico");
            }
        };
    }

    /**
     * Calcula a variação percentual: ((V_atual - V_anterior) / V_anterior) * 100.
     * Trata casos de borda com divisão por zero de forma segura e elegante.
     */
    public static BigDecimal calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        BigDecimal cur = current != null ? current : BigDecimal.ZERO;
        BigDecimal prev = previous != null ? previous : BigDecimal.ZERO;

        if (prev.compareTo(BigDecimal.ZERO) == 0) {
            if (cur.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(100.0);
            } else if (cur.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.valueOf(-100.0);
            }
            return BigDecimal.ZERO;
        }

        if (cur.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(-100.0);
        }

        BigDecimal diff = cur.subtract(prev);
        return diff.multiply(BigDecimal.valueOf(100))
                .divide(prev.abs(), 1, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o Ticket Médio Comercial: Receita Total / Total de Transações.
     */
    public static BigDecimal calculateAverageTicket(BigDecimal totalRevenue, long transactionCount) {
        if (totalRevenue == null || transactionCount <= 0 || totalRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return totalRevenue.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula a Margem Bruta Estimada (%): (Lucro Bruto / Receita Total) * 100.
     */
    public static BigDecimal calculateGrossMarginPercentage(BigDecimal grossProfit, BigDecimal totalRevenue) {
        if (grossProfit == null || totalRevenue == null || totalRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return grossProfit.multiply(BigDecimal.valueOf(100))
                .divide(totalRevenue, 1, RoundingMode.HALF_UP);
    }

    /**
     * Formata o percentual de variação de forma limpa, com sinal explícito (+/-).
     */
    public static String formatPercentage(BigDecimal percent) {
        if (percent == null || percent.compareTo(BigDecimal.ZERO) == 0) {
            return "0.0%";
        }
        if (percent.compareTo(BigDecimal.ZERO) > 0) {
            return String.format(java.util.Locale.US, "+%.1f%%", percent.doubleValue());
        }
        return String.format(java.util.Locale.US, "%.1f%%", percent.doubleValue());
    }

    /**
     * Formata o valor do Ticket Médio para exibição em Meticais.
     */
    public static String formatAverageTicket(BigDecimal avgTicket) {
        BigDecimal val = avgTicket != null ? avgTicket : BigDecimal.ZERO;
        return String.format("%,.2f MT/venda", val);
    }
}
