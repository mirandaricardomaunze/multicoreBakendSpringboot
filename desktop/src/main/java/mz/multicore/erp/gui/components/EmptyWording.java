package mz.multicore.erp.gui.components;

/**
 * Turns a table card title into the sentence of its empty state (docs/ESTADO_VAZIO_SPEC.md):
 * "Cotações" → "Sem cotações.", "Faturas de Compra Registadas" → "Sem faturas de compra registadas.".
 *
 * <p>Only plural titles are turned, since "Sem" needs a plural noun; any other title keeps the generic
 * "Sem registos.". Acronyms (IVA, NUIT, POS) keep their capitals.
 */
final class EmptyWording {

    private EmptyWording() {
    }

    static String fromTitle(String title) {
        if (title == null) return null;
        String clean = title.strip()
                .replaceAll("\\s+(—|-|\\(|/).*$", "")   // "Movimentos — Todos os…" → "Movimentos"
                .replace(" & ", " e ")
                .replaceAll("[:.]+$", "");
        if (clean.isEmpty()) return null;
        String[] words = clean.split("\\s+");
        if (!isPlural(words[0])) return null;
        StringBuilder out = new StringBuilder("Sem");
        for (String word : words) out.append(' ').append(isAcronym(word) ? word : word.toLowerCase(java.util.Locale.ROOT));
        return out.append('.').toString();
    }

    private static boolean isPlural(String word) {
        String w = word.toLowerCase(java.util.Locale.ROOT);
        return w.length() > 3 && w.endsWith("s") && Character.isLetter(w.charAt(0)) && !isAcronym(word);
    }

    private static boolean isAcronym(String word) {
        return word.length() > 1 && word.equals(word.toUpperCase(java.util.Locale.ROOT))
                && word.chars().anyMatch(Character::isLetter);
    }
}
