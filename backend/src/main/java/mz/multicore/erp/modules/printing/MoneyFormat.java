package mz.multicore.erp.modules.printing;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Money formatter for Metical. Keeps formatting consistent across all PDFs and
 * any future GUI label. Don't hand-format BigDecimal anywhere else.
 */
public final class MoneyFormat {

    private static final DecimalFormatSymbols SYMBOLS;
    private static final DecimalFormat AMOUNT;
    private static final DecimalFormat QTY;

    static {
        SYMBOLS = new DecimalFormatSymbols(new Locale("pt", "MZ"));
        SYMBOLS.setDecimalSeparator(',');
        SYMBOLS.setGroupingSeparator('.');
        AMOUNT = new DecimalFormat("#,##0.00", SYMBOLS);
        QTY = new DecimalFormat("#,##0.###", SYMBOLS);
    }

    private MoneyFormat() {}

    public static String format(BigDecimal value) {
        if (value == null) return "0,00 " + PdfTheme.CURRENCY_CODE;
        return AMOUNT.format(value) + " " + PdfTheme.CURRENCY_CODE;
    }

    public static String formatPlain(BigDecimal value) {
        if (value == null) return "0,00";
        return AMOUNT.format(value);
    }

    public static String formatQty(BigDecimal value) {
        if (value == null) return "0";
        return QTY.format(value);
    }
}
