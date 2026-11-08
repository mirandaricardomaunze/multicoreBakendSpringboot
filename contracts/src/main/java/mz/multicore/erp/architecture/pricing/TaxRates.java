package mz.multicore.erp.architecture.pricing;

import java.math.BigDecimal;

/**
 * Single source of truth for system VAT rates.
 *
 * Mozambique's standard VAT rate has been 16% since 2023. Apply a fixed rate to every line;
 * the rate does not depend on the customer or supplier NUIT.
 *
 * When exempt or reduced-rate products are supported, move the rate to {@code Product} and read
 * it there; this constant will then be only the default value.
 */
public final class TaxRates {

    /** Standard VAT rate applied by default (16%). */
    public static final BigDecimal STANDARD_VAT = new BigDecimal("0.16");

    private TaxRates() {}
}
