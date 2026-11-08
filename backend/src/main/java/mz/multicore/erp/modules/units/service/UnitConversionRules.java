package mz.multicore.erp.modules.units.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.units.model.UnitDimension;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure unit conversion rules (SA-C02), free of I/O so they can be tested in isolation.
 * Domain quantities are always in the base unit; these rules only convert user input.
 */
public final class UnitConversionRules {

    /** Scale of stored factors ({@code numeric(18,6)}). */
    public static final int FACTOR_SCALE = 6;

    private UnitConversionRules() {
    }

    /**
     * Fixed factor between two units of the same physical dimension (g to kg = 0.001), or {@code null}
     * when the conversion depends on the product (packages or different dimensions).
     */
    public static BigDecimal fixedFactor(UnitDimension unitDimension, BigDecimal unitDimensionFactor,
                                         UnitDimension baseDimension, BigDecimal baseDimensionFactor) {
        if (unitDimension == null || unitDimension != baseDimension || !unitDimension.hasFixedConversion()) {
            return null;
        }
        if (unitDimensionFactor == null || baseDimensionFactor == null || baseDimensionFactor.signum() <= 0) {
            return null;
        }
        return unitDimensionFactor.divide(baseDimensionFactor, FACTOR_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    /** Validates a factor entered by the user. */
    public static BigDecimal requireValidFactor(BigDecimal factor, int baseDecimals, String unitName, String baseName) {
        if (factor == null || factor.signum() <= 0) {
            throw new BusinessRuleException("Indique quantas " + baseName + " tem 1 " + unitName + " (valor maior que zero).");
        }
        BigDecimal scaled = factor.setScale(FACTOR_SCALE, RoundingMode.HALF_UP);
        if (scaled.signum() <= 0) {
            throw new BusinessRuleException("O factor de conversão é demasiado pequeno.");
        }
        if (baseDecimals == 0 && scaled.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleException("A unidade base (" + baseName + ") não aceita fracções: 1 " + unitName
                    + " tem de corresponder a um número inteiro de " + baseName + ".");
        }
        return scaled.stripTrailingZeros();
    }

    /**
     * Converts {@code quantity} from the alternative unit to the base unit. Rejects results with more
     * decimals than the base accepts, for example 1.5 bags that do not yield whole units.
     */
    public static BigDecimal toBase(BigDecimal quantity, BigDecimal factorToBase, int baseDecimals, String baseName) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("A quantidade tem de ser maior que zero.");
        }
        if (factorToBase == null || factorToBase.signum() <= 0) {
            throw new BusinessRuleException("A unidade não tem factor de conversão.");
        }
        BigDecimal raw = quantity.multiply(factorToBase);
        BigDecimal rounded = raw.setScale(baseDecimals, RoundingMode.HALF_UP);
        if (rounded.compareTo(raw) != 0) {
            throw new BusinessRuleException("A quantidade dá " + DisplayNumbers.qty(raw) + " " + baseName
                    + ", mas a unidade base só aceita " + baseDecimals + " casas decimais.");
        }
        return rounded;
    }
}
