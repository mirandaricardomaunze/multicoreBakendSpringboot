package mz.multicore.erp.modules.pricing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

/** Pure price-list rules (SA-C03), free of I/O so they can be tested in isolation. */
public final class PriceListRules {

    /** A tier: from {@code minQuantity} onwards the price is {@code price}. */
    public record Tier(BigDecimal minQuantity, BigDecimal price) {}

    private PriceListRules() {
    }

    /** Whether the list applies on the given date; a missing bound means "no limit". */
    public static boolean isApplicable(boolean active, LocalDate validFrom, LocalDate validTo, LocalDate date) {
        if (!active) return false;
        if (date == null) return true;
        if (validFrom != null && date.isBefore(validFrom)) return false;
        return validTo == null || !date.isAfter(validTo);
    }

    /** The tier with the highest minimum quantity reached by the line quantity. */
    public static Optional<Tier> pickTier(Collection<Tier> tiers, BigDecimal quantity) {
        if (tiers == null || tiers.isEmpty() || quantity == null) return Optional.empty();
        Tier best = null;
        for (Tier tier : tiers) {
            BigDecimal min = tier.minQuantity() == null ? BigDecimal.ZERO : tier.minQuantity();
            if (quantity.compareTo(min) < 0) continue;
            if (best == null || min.compareTo(best.minQuantity() == null ? BigDecimal.ZERO : best.minQuantity()) > 0) {
                best = new Tier(min, tier.price());
            }
        }
        return Optional.ofNullable(best);
    }
}
