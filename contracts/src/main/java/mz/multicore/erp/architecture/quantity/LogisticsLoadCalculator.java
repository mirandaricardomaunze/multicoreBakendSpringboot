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

    public static BigDecimal totalWeight(List<Input> inputs) {
        if (inputs == null || inputs.isEmpty()) return BigDecimal.ZERO;
        return inputs.stream().map(LogisticsLoadCalculator::lineWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Recomenda o porte de viatura e capacidade de transporte com base no peso total da carga.
     * Apoia o armazém e logística na alocação da viatura adequada para o carregamento.
     */
    public static String recommendedVehicleType(BigDecimal totalWeightKg) {
        if (totalWeightKg == null || totalWeightKg.signum() <= 0) {
            return "Sem peso registado (aferir na balança)";
        }
        if (totalWeightKg.compareTo(BigDecimal.valueOf(1000)) <= 0) {
            return "Veículo Ligeiro / Pick-up (até 1 Ton)";
        }
        if (totalWeightKg.compareTo(BigDecimal.valueOf(3500)) <= 0) {
            return "Comercial Ligeiro / Carrinha 3.5T (ex: Canter / Dyna / Isuzu)";
        }
        if (totalWeightKg.compareTo(BigDecimal.valueOf(10000)) <= 0) {
            return "Camião Médio (até 10 Ton - ex: Isuzu FTR / Hino 500)";
        }
        if (totalWeightKg.compareTo(BigDecimal.valueOf(28000)) <= 0) {
            return "Camião Pesado (até 28 Ton - Trucado / 3 Eixos)";
        }
        return "Carga Excedente / Requer Articulado Pesado ou Múltiplos Carros";
    }

    public static String formatWeightWithVehicle(BigDecimal totalWeightKg) {
        if (totalWeightKg == null || totalWeightKg.signum() <= 0) {
            return "0 kg";
        }
        BigDecimal tons = totalWeightKg.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        return totalWeightKg.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                + " kg (" + tons.stripTrailingZeros().toPlainString() + " Ton) — "
                + recommendedVehicleType(totalWeightKg);
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
