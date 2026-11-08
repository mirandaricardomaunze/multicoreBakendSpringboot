package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.format.DisplayLocale;
import mz.multicore.erp.gui.DashboardPanel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Deterministic calculator for trends, comparison periods, and executive analytics used by the
 * commercial and management dashboards.
 */
public final class DashboardTrendCalculator {

    /** Label used for the entire history, where no preceding period exists for comparison. */
    public static final String NO_COMPARISON_LABEL = "histórico";

    public static boolean hasComparison(String comparisonLabel) {
        return comparisonLabel != null && !NO_COMPARISON_LABEL.equals(comparisonLabel);
    }

    private DashboardTrendCalculator() {
    }

    /**
     * Closed date range [from, to]. Null values represent open boundaries.
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
     * Pair of time-aligned current and previous periods with a descriptive label.
     */
    public record ComparisonPeriod(DateRange current, DateRange previous, String comparisonLabel) {
    }

    /**
     * Resolves current and previous date ranges from the selected filter and anchor date.
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
                yield new ComparisonPeriod(current, null, NO_COMPARISON_LABEL);
            }
        };
    }

    /**
     * Calculates percentage change as ((current - previous) / previous) * 100.
     * Handles zero denominators without producing invalid numeric results.
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
     * Calculates the average commercial ticket as total revenue divided by transaction count.
     */
    public static BigDecimal calculateAverageTicket(BigDecimal totalRevenue, long transactionCount) {
        if (totalRevenue == null || transactionCount <= 0 || totalRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return totalRevenue.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates estimated gross margin as gross profit divided by total revenue.
     */
    public static BigDecimal calculateGrossMarginPercentage(BigDecimal grossProfit, BigDecimal totalRevenue) {
        if (grossProfit == null || totalRevenue == null || totalRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return grossProfit.multiply(BigDecimal.valueOf(100))
                .divide(totalRevenue, 1, RoundingMode.HALF_UP);
    }

    /**
     * Formats a percentage change with an explicit positive or negative sign.
     */
    public static String formatPercentage(BigDecimal percent) {
        if (percent == null || percent.compareTo(BigDecimal.ZERO) == 0) {
            return "0,0%";
        }
        if (percent.compareTo(BigDecimal.ZERO) > 0) {
            return String.format(DisplayLocale.NUMBERS, "+%.1f%%", percent.doubleValue());
        }
        return String.format(DisplayLocale.NUMBERS, "%.1f%%", percent.doubleValue());
    }

    /**
     * When the period is idle, the card shows the previous value instead of an uninformative zero.
     */
    public static String activityHint(BigDecimal current, BigDecimal previous, String comparisonLabel, String activeText) {
        boolean idle = current == null || current.signum() <= 0;
        if (!idle || comparisonLabel == null || !comparisonLabel.startsWith("vs ")) return activeText;
        String period = comparisonLabel.substring(3);
        return Character.toUpperCase(period.charAt(0)) + period.substring(1) + ": "
                + String.format("%,.2f MT", previous != null ? previous : BigDecimal.ZERO);
    }

    /**
     * Formats the average ticket for display in Mozambican meticais.
     */
    public static String formatAverageTicket(BigDecimal avgTicket) {
        BigDecimal val = avgTicket != null ? avgTicket : BigDecimal.ZERO;
        return String.format("%,.2f MT/venda", val);
    }
}
