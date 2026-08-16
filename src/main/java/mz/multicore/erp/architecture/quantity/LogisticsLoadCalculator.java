package mz.multicore.erp.architecture.quantity;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class LogisticsLoadCalculator {
    private LogisticsLoadCalculator() {}

    public record Input(BigDecimal quantity, BigDecimal grossUnitWeightKg) {}
    public record Share(BigDecimal lineWeightKg, BigDecimal quantityPercentage, BigDecimal weightPercentage) {}

    public static List<Share> calculate(List<Input> inputs) {
        BigDecimal totalQuantity = inputs.stream().map(LogisticsLoadCalculator::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeight = inputs.stream().map(LogisticsLoadCalculator::lineWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return inputs.stream().map(input -> new Share(lineWeight(input), percentage(quantity(input), totalQuantity),
                percentage(lineWeight(input), totalWeight))).toList();
    }

    public static void validateWeights(BigDecimal netKg, BigDecimal grossKg) {
        if (negative(netKg) || negative(grossKg)) throw new BusinessRuleException("Os pesos nao podem ser negativos.");
        if (netKg != null && grossKg != null && grossKg.compareTo(netKg) < 0) {
            throw new BusinessRuleException("O peso bruto deve ser igual ou superior ao peso liquido.");
        }
    }

    private static BigDecimal quantity(Input input) {
        return input.quantity() == null ? BigDecimal.ZERO : input.quantity();
    }
    private static BigDecimal lineWeight(Input input) {
        BigDecimal weight = input.grossUnitWeightKg() == null ? BigDecimal.ZERO : input.grossUnitWeightKg();
        return quantity(input).multiply(weight).setScale(3, RoundingMode.HALF_UP);
    }
    private static BigDecimal percentage(BigDecimal value, BigDecimal total) {
        if (total.signum() == 0) return BigDecimal.ZERO.setScale(2);
        return value.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }
    private static boolean negative(BigDecimal value) { return value != null && value.signum() < 0; }
}
