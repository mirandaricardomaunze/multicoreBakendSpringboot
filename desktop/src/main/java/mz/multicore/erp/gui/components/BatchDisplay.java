package mz.multicore.erp.gui.components;

import java.time.LocalDate;
import java.util.Locale;

/** Esconde os valores internos de lote: número gerado pelo sistema e validade de "sem validade". */
public final class BatchDisplay {

    private static final String NONE = "—";

    private BatchDisplay() {}

    public static String number(String raw) {
        if (raw == null || raw.isBlank() || "-".equals(raw)) return NONE;
        String upper = raw.toUpperCase(Locale.ROOT);
        return upper.startsWith("AUTO-") || upper.startsWith("LEGACY") ? NONE : raw;
    }

    /** Validade real do lote, ou {@code null} quando o artigo não tem validade. */
    public static LocalDate expiry(LocalDate raw) {
        return raw == null || raw.getYear() >= 9999 ? null : raw;
    }
}
