package mz.multicore.erp.modules.variants.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pure variant rules: clean value lists, the combinations to create and the code and name of each variant. */
public final class VariantRules {

    public record Combination(String value1, String value2) {}

    private VariantRules() {}

    /** Trimmed, without blanks and without repeated values (ignoring case), in the order typed. */
    public static List<String> clean(List<String> values) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        if (values == null) return result;
        for (String v : values) {
            if (v == null || v.isBlank()) continue;
            String trimmed = v.trim();
            if (seen.add(trimmed.toLowerCase(Locale.ROOT))) result.add(trimmed);
        }
        return result;
    }

    /** Every combination of the two lists; with an empty second list, one per first value. */
    public static List<Combination> combinations(List<String> values1, List<String> values2) {
        List<Combination> result = new ArrayList<>();
        for (String a : values1) {
            if (values2 == null || values2.isEmpty()) result.add(new Combination(a, null));
            else for (String b : values2) result.add(new Combination(a, b));
        }
        return result;
    }

    public static String sku(String prefix, Combination c) {
        return prefix.toUpperCase(Locale.ROOT) + "-" + code(c.value1()) + (c.value2() == null ? "" : "-" + code(c.value2()));
    }

    public static String name(String family, Combination c) {
        return family + " - " + c.value1() + (c.value2() == null ? "" : " / " + c.value2());
    }

    /** "Azul Marinho" -> "AZULMARINHO", "1/2 kg" -> "12KG": letters and digits only, no accents. */
    static String code(String value) {
        String plain = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String code = plain.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return code.isEmpty() ? "X" : code.substring(0, Math.min(12, code.length()));
    }
}
