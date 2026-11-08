package mz.multicore.erp.architecture.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Numbers displayed to users in errors, documents, and notices: {@code 1.460,00} and
 * {@code 2,5}, never {@code 1460.00} or {@code 2.5}. See docs/FORMATO_NUMERICO_SPEC.md (FN-11).
 *
 * <p>Machine-readable files (SAF-T, bank files, and idempotency keys) retain decimal points and do
 * not use this class. {@link DecimalFormat} is not thread-safe, so each call creates one.</p>
 */
public final class DisplayNumbers {

    private DisplayNumbers() {
    }

    /** Monetary value with two decimal places: {@code 1.460,00}. Null is treated as zero. */
    public static String money(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return format("#,##0.00").format(safe);
    }

    /** Quantity without insignificant zeros, up to three decimal places: {@code 2,5}, {@code 1.000}, {@code 0,25}. */
    public static String qty(BigDecimal value) {
        return format("#,##0.###").format(value == null ? BigDecimal.ZERO : value);
    }

    private static DecimalFormat format(String pattern) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(DisplayLocale.NUMBERS);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat(pattern, symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}
