package mz.multicore.erp.modules.bulkstock.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.modules.bulkstock.model.BulkMeasurementMethod;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Pure bulk-pile rules (SA-C17): pile volumes, differences and tolerance, free of I/O. */
public final class BulkMeasurementRules {

    /** Largest dimension accepted in metres; anything above is almost certainly a typing error. */
    static final BigDecimal MAX_DIMENSION_M = new BigDecimal("500");
    private static final BigDecimal PI = new BigDecimal("3.14159265358979");
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal THREE = BigDecimal.valueOf(3);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private BulkMeasurementRules() {
    }

    /** Pile volume in cubic metres, rounded to three decimals. */
    public static BigDecimal volume(BulkMeasurementMethod method, BigDecimal d1, BigDecimal d2, BigDecimal d3) {
        return switch (method) {
            case DIRECT -> throw new BusinessRuleException("A quantidade directa não tem volume a calcular.");
            case BOX -> round(dim(d1, "comprimento").multiply(dim(d2, "largura")).multiply(dim(d3, "altura")));
            case HEAP -> round(dim(d1, "comprimento").multiply(dim(d2, "largura")).multiply(dim(d3, "altura"))
                    .divide(TWO, MathContext.DECIMAL64));
            case CONE -> {
                BigDecimal radius = dim(d1, "diâmetro").divide(TWO, MathContext.DECIMAL64);
                yield round(PI.multiply(radius).multiply(radius).multiply(dim(d2, "altura"))
                        .divide(THREE, MathContext.DECIMAL64));
            }
        };
    }

    /** Physical discrepancy: what is on the pile, minus sold material awaiting pickup, minus the system stock. */
    public static BigDecimal difference(BigDecimal measured, BigDecimal pendingPickup, BigDecimal system) {
        return nz(measured).subtract(nz(pendingPickup)).subtract(nz(system));
    }

    /** Absolute difference as a percentage of the system stock; 100% when the system holds nothing. */
    public static BigDecimal differencePercent(BigDecimal difference, BigDecimal system) {
        if (nz(difference).signum() == 0) return BigDecimal.ZERO.setScale(2);
        if (nz(system).signum() <= 0) return HUNDRED.setScale(2);
        return difference.abs().multiply(HUNDRED).divide(system, 2, RoundingMode.HALF_UP);
    }

    public static boolean withinTolerance(BigDecimal differencePercent, BigDecimal tolerancePercent) {
        return nz(differencePercent).compareTo(nz(tolerancePercent)) <= 0;
    }

    public static BigDecimal requireTolerance(BigDecimal tolerance) {
        if (tolerance == null || tolerance.signum() < 0 || tolerance.compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new BusinessRuleException("A tolerância de perda natural tem de estar entre 0 e 50 por cento.");
        }
        return tolerance.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dim(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessRuleException("Indique o " + name + " do monte em metros (maior que zero).");
        }
        if (value.compareTo(MAX_DIMENSION_M) > 0) {
            throw new BusinessRuleException("O " + name + " de " + DisplayNumbers.qty(value)
                    + " m parece um engano. Confirme a medida em metros.");
        }
        return value;
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
