package mz.multicore.erp.architecture.format;

import java.util.Locale;

/**
 * Formato dos valores mostrados ao utilizador: {@code 1.500,50}. Não usa {@code pt-MZ} porque o JDK
 * escreve esse locale com espaço ({@code 1 500,50}). Ver docs/FORMATO_NUMERICO_SPEC.md.
 */
public final class DisplayLocale {
    public static final Locale NUMBERS = Locale.forLanguageTag("pt-BR");

    private DisplayLocale() {
    }
}
