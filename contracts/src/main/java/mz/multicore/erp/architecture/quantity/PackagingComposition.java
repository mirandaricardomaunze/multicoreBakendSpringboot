package mz.multicore.erp.architecture.quantity;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

/** Immutable composition of a product's commercial case. */
public record PackagingComposition(int packagesPerBox, int unitsPerPackage, int unitsPerBox) {

    public static PackagingComposition of(int packagesPerBox, int unitsPerPackage) {
        if (packagesPerBox <= 0) {
            throw new BusinessRuleException("As embalagens por caixa devem ser maiores que zero.");
        }
        if (unitsPerPackage <= 0) {
            throw new BusinessRuleException("As unidades por embalagem devem ser maiores que zero.");
        }
        try {
            return new PackagingComposition(packagesPerBox, unitsPerPackage,
                    Math.multiplyExact(packagesPerBox, unitsPerPackage));
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("A composição da caixa excede a quantidade máxima permitida.");
        }
    }

    /** Compatibility with products and clients that only know units per case. */
    public static PackagingComposition legacy(int unitsPerBox) {
        return of(Math.max(1, unitsPerBox), 1);
    }
}

