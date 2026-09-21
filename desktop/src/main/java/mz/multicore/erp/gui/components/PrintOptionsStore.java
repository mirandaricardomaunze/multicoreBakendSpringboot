package mz.multicore.erp.gui.components;

import java.util.prefs.Preferences;

/**
 * Lembra as escolhas de impressão por <b>família de documento</b>: as etiquetas saem sempre na
 * impressora térmica e as facturas na laser, e o operador não devia ter de o repetir a cada venda.
 *
 * <p>À prova de falha, como o {@code NotificationReadStore}: se as {@code Preferences} não
 * estiverem disponíveis (perfil sem escrita, ambiente de teste), funciona só em memória — nunca
 * impede a impressão.</p>
 */
public final class PrintOptionsStore {

    private static final String NODE = "mz/multicore/erp/print";

    private final Preferences prefs; // nulo = modo memória

    PrintOptionsStore(Preferences prefs) {
        this.prefs = prefs;
    }

    public static PrintOptionsStore userStore() {
        return new PrintOptionsStore(safeNode());
    }

    /** Store desligado das Preferences — usado nos testes e quando o perfil não permite escrita. */
    public static PrintOptionsStore inMemory() {
        return new PrintOptionsStore(null);
    }

    /**
     * Família a que um nome de ficheiro pertence: descarta os segmentos finais que identificam o
     * documento concreto (número, ano, id). {@code "guia-remessa-2026/14"} → {@code "guia-remessa"}.
     */
    public static String familyOf(String baseName) {
        if (baseName == null || baseName.isBlank()) return "documento";
        String[] parts = baseName.split("-");
        StringBuilder family = new StringBuilder();
        for (String part : parts) {
            String piece = part.trim();
            if (piece.isEmpty() || piece.chars().anyMatch(Character::isDigit)) break;
            if (family.length() > 0) family.append('-');
            family.append(piece.toLowerCase());
        }
        String result = family.toString().replaceAll("[^a-z-]", "");
        return result.isBlank() ? "documento" : result;
    }

    /** Últimas opções usadas nesta família, ou os defeitos se ainda não houver escolha guardada. */
    public PrintOptions load(String baseName, String defaultPrinter) {
        PrintOptions fallback = PrintOptions.defaults(defaultPrinter);
        if (prefs == null) return fallback;
        String key = familyOf(baseName);
        try {
            String printer = prefs.get(key + ".printer", defaultPrinter);
            return new PrintOptions(
                    printer == null || printer.isBlank() ? defaultPrinter : printer,
                    prefs.getInt(key + ".copies", 1),
                    PrintOptions.Orientation.byName(prefs.get(key + ".orientation", ""), fallback.orientation()),
                    PrintOptions.Fit.byName(prefs.get(key + ".fit", ""), fallback.fit()),
                    "", // o intervalo de páginas é do documento concreto, não da família
                    prefs.getBoolean(key + ".grayscale", false));
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    /** Guarda as escolhas depois de uma impressão bem sucedida. */
    public void save(String baseName, PrintOptions options) {
        if (prefs == null || options == null) return;
        String key = familyOf(baseName);
        try {
            if (options.printerName() != null) prefs.put(key + ".printer", options.printerName());
            prefs.putInt(key + ".copies", options.copies());
            prefs.put(key + ".orientation", options.orientation().name());
            prefs.put(key + ".fit", options.fit().name());
            prefs.putBoolean(key + ".grayscale", options.grayscale());
        } catch (RuntimeException ex) {
            // Preferences indisponíveis não podem rebentar a impressão.
        }
    }

    private static Preferences safeNode() {
        try {
            return Preferences.userRoot().node(NODE);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
