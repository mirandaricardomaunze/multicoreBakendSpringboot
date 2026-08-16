package mz.multicore.erp.architecture.quantity;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import java.math.BigDecimal;

public record PackageQuantity(long boxes, long looseUnits, long totalUnits, int unitsPerBox) {
    public static PackageQuantity fromPackages(long boxes, long looseUnits, int unitsPerBox) {
        requireValidFactor(unitsPerBox);
        if (boxes < 0 || looseUnits < 0 || looseUnits >= unitsPerBox) {
            throw new BusinessRuleException("As caixas devem ser positivas e as unidades soltas inferiores a uma caixa.");
        }
        return new PackageQuantity(boxes, looseUnits,
                Math.addExact(Math.multiplyExact(boxes, unitsPerBox), looseUnits), unitsPerBox);
    }

    public static PackageQuantity fromTotal(long totalUnits, int unitsPerBox) {
        requireValidFactor(unitsPerBox);
        if (totalUnits < 0) throw new BusinessRuleException("A quantidade total nao pode ser negativa.");
        return new PackageQuantity(totalUnits / unitsPerBox, totalUnits % unitsPerBox, totalUnits, unitsPerBox);
    }

    public String label() {
        if (boxes == 0) return looseUnits + " un";
        if (looseUnits == 0) return boxes + " cx";
        return boxes + " cx + " + looseUnits + " un";
    }

    public static String label(BigDecimal total, int unitsPerBox) {
        if (total == null) return "0 un";
        try {
            return fromTotal(total.longValueExact(), unitsPerBox).label();
        } catch (ArithmeticException ex) {
            return total.stripTrailingZeros().toPlainString() + " un";
        }
    }

    private static void requireValidFactor(int unitsPerBox) {
        if (unitsPerBox <= 0) throw new BusinessRuleException("Configure as unidades por caixa do produto.");
    }
}
