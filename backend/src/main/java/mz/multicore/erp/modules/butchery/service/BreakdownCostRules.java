package mz.multicore.erp.modules.butchery.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure rules of a carcass breakdown. The cost of the carcass is shared by the cuts in proportion to their sale
 * value (kg x sale price), the way butchers price it: a kg of fillet carries more cost than a kg of bones. Cuts
 * without a sale price carry no cost; if no cut has a price, the cost is shared by weight. The rounding difference
 * goes to the cut with the largest value, so the cuts always add up to the cost of the carcass.
 */
public final class BreakdownCostRules {

    public record Cut(Long productId, BigDecimal kg, BigDecimal unitPrice) {}

    public record Allocation(Long productId, BigDecimal cost, BigDecimal costPerKg) {}

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private BreakdownCostRules() {}

    public static List<Allocation> allocate(BigDecimal totalCost, List<Cut> cuts) {
        BigDecimal cost = nz(totalCost).setScale(2, RoundingMode.HALF_UP);
        if (cuts.isEmpty()) return List.of();
        BigDecimal valueSum = cuts.stream().map(BreakdownCostRules::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean byWeight = valueSum.signum() == 0;
        List<BigDecimal> weights = cuts.stream().map(c -> byWeight ? nz(c.kg()) : value(c)).toList();
        BigDecimal base = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> shares = new ArrayList<>();
        int largest = 0;
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < cuts.size(); i++) {
            BigDecimal share = base.signum() == 0 ? BigDecimal.ZERO
                    : cost.multiply(weights.get(i)).divide(base, 2, RoundingMode.HALF_UP);
            shares.add(share);
            allocated = allocated.add(share);
            if (weights.get(i).compareTo(weights.get(largest)) > 0) largest = i;
        }
        shares.set(largest, shares.get(largest).add(cost.subtract(allocated)));
        List<Allocation> result = new ArrayList<>();
        for (int i = 0; i < cuts.size(); i++) {
            BigDecimal kg = nz(cuts.get(i).kg());
            BigDecimal perKg = kg.signum() == 0 ? BigDecimal.ZERO : shares.get(i).divide(kg, 4, RoundingMode.HALF_UP);
            result.add(new Allocation(cuts.get(i).productId(), shares.get(i), perKg));
        }
        return result;
    }

    /** Refuses a breakdown that gives more meat than the carcass weighed. */
    public static void requireOutputWithinInput(BigDecimal inputKg, BigDecimal outputKg) {
        if (outputKg.compareTo(inputKg) > 0) {
            throw new BusinessRuleException("Os cortes somam " + DisplayNumbers.qty(outputKg) + " kg, mais do que a carcaça ("
                    + DisplayNumbers.qty(inputKg) + " kg). Confirme as pesagens.");
        }
    }

    public static BigDecimal percentOf(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) return BigDecimal.ZERO;
        return nz(part).multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }

    public static boolean lossAboveTolerance(BigDecimal lossPct, BigDecimal tolerancePct) {
        return tolerancePct != null && nz(lossPct).compareTo(tolerancePct) > 0;
    }

    private static BigDecimal value(Cut cut) {
        return nz(cut.kg()).multiply(nz(cut.unitPrice()));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
