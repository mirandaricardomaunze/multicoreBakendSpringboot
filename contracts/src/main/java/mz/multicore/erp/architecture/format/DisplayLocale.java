package mz.multicore.erp.architecture.format;

import java.util.Locale;

/**
 * Format for values displayed to users: {@code 1.500,50}. This does not use {@code pt-MZ} because
 * the JDK formats that locale with a space ({@code 1 500,50}). See docs/FORMATO_NUMERICO_SPEC.md.
 */
public final class DisplayLocale {
    public static final Locale NUMBERS = Locale.forLanguageTag("pt-BR");

    private DisplayLocale() {
    }
}
