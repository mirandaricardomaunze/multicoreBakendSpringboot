package mz.multicore.erp.modules.productionplan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Pure rules of the day plan: ingredient quantities from the recipe and the suggestion from past sales. */
public final class ProductionPlanRules {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ProductionPlanRules() {}

    /** Ingredient for {@code quantity} units of a recipe that yields {@code baseQuantity}, scrap included. */
    public static BigDecimal ingredient(BigDecimal quantity, BigDecimal baseQuantity, BigDecimal requiredPerBase, BigDecimal scrapPct) {
        if (quantity == null || quantity.signum() <= 0 || requiredPerBase == null) return BigDecimal.ZERO;
        BigDecimal base = baseQuantity == null || baseQuantity.signum() <= 0 ? BigDecimal.ONE : baseQuantity;
        BigDecimal scrap = scrapPct == null ? BigDecimal.ZERO : scrapPct;
        return quantity.multiply(requiredPerBase).multiply(HUNDRED.add(scrap))
                .divide(base.multiply(HUNDRED), 4, RoundingMode.HALF_UP);
    }

    /**
     * Average sold on the same weekday as {@code day} over the last {@code weeks} weeks, rounded up to a whole unit.
     * Weeks with no sale count as zero, so a product that stopped selling is not over-produced.
     */
    public static BigDecimal suggestion(Map<LocalDate, BigDecimal> soldByDay, LocalDate day, int weeks) {
        DayOfWeek weekday = day.getDayOfWeek();
        BigDecimal total = BigDecimal.ZERO;
        for (int w = 1; w <= weeks; w++) {
            LocalDate past = day.minusWeeks(w);
            if (past.getDayOfWeek() == weekday) total = total.add(soldByDay.getOrDefault(past, BigDecimal.ZERO));
        }
        if (weeks <= 0) return BigDecimal.ZERO;
        return total.divide(BigDecimal.valueOf(weeks), 0, RoundingMode.CEILING);
    }

    public static BigDecimal remaining(BigDecimal planned, BigDecimal produced) {
        BigDecimal left = (planned == null ? BigDecimal.ZERO : planned).subtract(produced == null ? BigDecimal.ZERO : produced);
        return left.max(BigDecimal.ZERO);
    }

    public static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
