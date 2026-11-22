package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.format.DisplayNumbers;

import java.math.BigDecimal;

/**
 * Money formatter for Metical in printed documents. Delegates to {@link DisplayNumbers}, the single
 * place that writes numbers for people. Don't hand-format BigDecimal anywhere else.
 */
public final class MoneyFormat {

    private MoneyFormat() {}

    public static String format(BigDecimal value) {
        return formatPlain(value) + " " + PdfTheme.CURRENCY_CODE;
    }

    /** Valor com o código da moeda do documento (ex.: "100,00 USD"), para documentos que não estão em meticais. */
    public static String format(BigDecimal value, String currencyCode) {
        return formatPlain(value) + " " + (currencyCode == null || currencyCode.isBlank()
                ? PdfTheme.CURRENCY_CODE : currencyCode);
    }

    public static String formatPlain(BigDecimal value) {
        return DisplayNumbers.money(value);
    }

    public static String formatQty(BigDecimal value) {
        return value == null ? "0" : DisplayNumbers.qty(value);
    }
}
