package mz.multicore.erp.gui.tv;

import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Indicadores de desempenho de um período (empresa ou unidade). Sem Swing: só cálculo.
 *
 * @param elapsedDays dias já decorridos do período (inclui hoje)
 * @param totalDays   duração total do período, para o ritmo esperado e a previsão de fecho
 */
public record ExecutiveTvMetrics(BigDecimal billed, BigDecimal goal, long invoiceCount, int elapsedDays, int totalDays) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static ExecutiveTvMetrics of(List<InvoiceDTO> invoices, Long unitId, BigDecimal goal,
                                        ExecutiveTvDashboardDialog.Period period, LocalDate today) {
        List<InvoiceDTO> safe = invoices != null ? invoices : List.of();
        BigDecimal billed = ExecutiveTvCalculationHelper.calculateTotalBilled(safe, unitId);
        long count = safe.stream().filter(inv -> ExecutiveTvCalculationHelper.belongsTo(inv, unitId)).count();
        int[] days = periodDays(period, today);
        return new ExecutiveTvMetrics(billed, goal != null ? goal : BigDecimal.ZERO, count, days[0], days[1]);
    }

    /** {dias decorridos, dias totais} do período a que o filtro de faturas corresponde. */
    static int[] periodDays(ExecutiveTvDashboardDialog.Period period, LocalDate today) {
        return switch (period) {
            case HOJE -> new int[]{1, 1};
            case ESTA_SEMANA -> new int[]{7, 7};
            case ESTE_MES -> new int[]{today.getDayOfMonth(), today.lengthOfMonth()};
            case ESTE_ANO -> new int[]{today.getDayOfYear(), today.lengthOfYear()};
        };
    }

    public boolean hasGoal() {
        return goal.compareTo(BigDecimal.ZERO) > 0;
    }

    public double percent() {
        return hasGoal() ? billed.multiply(HUNDRED).divide(goal, 1, RoundingMode.HALF_UP).doubleValue() : 0.0;
    }

    public GoalStatus status() {
        return GoalStatus.of(hasGoal(), percent());
    }

    /** Percentagem da meta que já devia estar feita hoje, a ritmo constante. */
    public double expectedPercent() {
        return totalDays <= 0 ? 0.0 : Math.min(100.0, elapsedDays * 100.0 / totalDays);
    }

    /** Positivo: acima da meta; negativo: quanto falta. */
    public BigDecimal difference() {
        return billed.subtract(goal);
    }

    public BigDecimal averageTicket() {
        return invoiceCount == 0 ? BigDecimal.ZERO
                : billed.divide(BigDecimal.valueOf(invoiceCount), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal dailyRate() {
        return billed.divide(BigDecimal.valueOf(Math.max(1, elapsedDays)), 2, RoundingMode.HALF_UP);
    }

    /** Fecho previsto do período se o ritmo diário actual se mantiver. */
    public BigDecimal projection() {
        return dailyRate().multiply(BigDecimal.valueOf(Math.max(1, totalDays)));
    }

    public double projectionPercent() {
        return hasGoal() ? projection().multiply(HUNDRED).divide(goal, 1, RoundingMode.HALF_UP).doubleValue() : 0.0;
    }

    /** Venda diária necessária nos dias que faltam para chegar à meta (zero se já foi atingida). */
    public BigDecimal requiredDailyRate() {
        if (!hasGoal() || difference().signum() >= 0) return BigDecimal.ZERO;
        int remaining = Math.max(1, totalDays - elapsedDays);
        return difference().abs().divide(BigDecimal.valueOf(remaining), 2, RoundingMode.HALF_UP);
    }
}
