package mz.multicore.erp.modules.expiry.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Pure expiry rules. Stages drive the colours and the order of the screen. A markdown rule reads "from N days
 * before expiry, X% off"; for a lot, the rules whose window has started apply and the largest discount wins.
 * Rules of the product category take precedence over general rules, so "Carne: 1 dia, 40%" is not overruled by
 * a softer general rule.
 */
public final class ExpiryRules {

    public record Rule(Long categoryId, int daysBefore, BigDecimal percent) {}

    private ExpiryRules() {}

    public static long daysLeft(LocalDate today, LocalDate expiration) {
        return ChronoUnit.DAYS.between(today, expiration);
    }

    public static String stage(long daysLeft) {
        if (daysLeft < 0) return "EXPIRED";
        if (daysLeft == 0) return "TODAY";
        if (daysLeft <= 2) return "SOON";
        if (daysLeft <= 7) return "WEEK";
        return "LATER";
    }

    /** Suggested discount for a lot, or {@code null} when no rule applies (or the lot already expired). */
    public static BigDecimal suggested(List<Rule> rules, Long categoryId, long daysLeft) {
        if (daysLeft < 0) return null;
        BigDecimal specific = best(rules, daysLeft, categoryId, true);
        return specific != null ? specific : best(rules, daysLeft, categoryId, false);
    }

    private static BigDecimal best(List<Rule> rules, long daysLeft, Long categoryId, boolean specific) {
        BigDecimal best = null;
        for (Rule rule : rules) {
            boolean matches = specific ? categoryId != null && categoryId.equals(rule.categoryId()) : rule.categoryId() == null;
            if (!matches || daysLeft > rule.daysBefore()) continue;
            if (best == null || rule.percent().compareTo(best) > 0) best = rule.percent();
        }
        return best;
    }
}
