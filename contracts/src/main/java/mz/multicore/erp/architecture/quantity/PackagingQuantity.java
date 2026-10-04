package mz.multicore.erp.architecture.quantity;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import java.math.BigDecimal;

/** Quantidade decomposta nos três níveis comerciais, mantendo um único total em unidades. */
public record PackagingQuantity(
        long boxes,
        long packages,
        long looseUnits,
        long totalUnits,
        PackagingComposition composition
) {
    public static PackagingQuantity fromPackaging(long boxes, long packages, long looseUnits,
                                                   PackagingComposition composition) {
        requireComposition(composition);
        if (boxes < 0 || packages < 0 || looseUnits < 0) {
            throw new BusinessRuleException("Caixas, embalagens e unidades não podem ser negativas.");
        }
        if (packages >= composition.packagesPerBox()) {
            throw new BusinessRuleException("As embalagens soltas devem ser inferiores a uma caixa.");
        }
        if (looseUnits >= composition.unitsPerPackage()) {
            throw new BusinessRuleException("As unidades soltas devem ser inferiores a uma embalagem.");
        }
        try {
            long total = Math.addExact(
                    Math.addExact(Math.multiplyExact(boxes, composition.unitsPerBox()),
                            Math.multiplyExact(packages, composition.unitsPerPackage())),
                    looseUnits);
            return new PackagingQuantity(boxes, packages, looseUnits, total, composition);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("A quantidade indicada excede o máximo permitido.");
        }
    }

    public static PackagingQuantity fromTotal(long totalUnits, PackagingComposition composition) {
        requireComposition(composition);
        if (totalUnits < 0) {
            throw new BusinessRuleException("A quantidade total não pode ser negativa.");
        }
        long boxes = totalUnits / composition.unitsPerBox();
        long remainder = totalUnits % composition.unitsPerBox();
        long packages = remainder / composition.unitsPerPackage();
        long looseUnits = remainder % composition.unitsPerPackage();
        return new PackagingQuantity(boxes, packages, looseUnits, totalUnits, composition);
    }

    public String label() {
        java.util.List<String> parts = new java.util.ArrayList<>(3);
        if (boxes > 0) parts.add(boxes + " cx");
        if (packages > 0) parts.add(packages + " emb");
        if (looseUnits > 0 || parts.isEmpty()) parts.add(looseUnits + " un");
        return String.join(" + ", parts);
    }

    public static String label(BigDecimal totalUnits, PackagingComposition composition) {
        if (totalUnits == null) return "0 un";
        try {
            return fromTotal(totalUnits.longValueExact(), composition).label();
        } catch (ArithmeticException exception) {
            return totalUnits.stripTrailingZeros().toPlainString() + " un";
        }
    }

    private static void requireComposition(PackagingComposition composition) {
        if (composition == null) {
            throw new BusinessRuleException("Configure a composição da caixa do produto.");
        }
    }
}
