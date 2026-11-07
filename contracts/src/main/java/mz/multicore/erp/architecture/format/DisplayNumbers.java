package mz.multicore.erp.architecture.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Números escritos para o utilizador — mensagens de erro, documentos, avisos: {@code 1.460,00} e
 * {@code 2,5}, nunca {@code 1460.00} nem {@code 2.5}. Ver docs/FORMATO_NUMERICO_SPEC.md (FN-11).
 *
 * <p>Ficheiros para máquinas (SAF-T, ficheiro bancário, chaves de idempotência) continuam com ponto
 * e não passam por aqui. {@link DecimalFormat} não é thread-safe: cria-se um por chamada.</p>
 */
public final class DisplayNumbers {

    private DisplayNumbers() {
    }

    /** Valor monetário com duas casas: {@code 1.460,00}. Nulo vale zero. */
    public static String money(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return format("#,##0.00").format(safe);
    }

    /** Quantidade sem zeros inúteis, até três casas: {@code 2,5}, {@code 1.000}, {@code 0,25}. */
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
