package mz.multicore.erp.architecture.version;

/**
 * Compares {@code MAJOR.MINOR.PATCH} versions, such as {@code 1.4.2}.
 *
 * <p><b>Single source</b> for determining whether one version predates another. Lexicographic
 * comparison incorrectly places {@code "1.10.0" < "1.9.0"} and can block a newer client. This
 * implementation compares each numeric component.
 *
 * <p>Intentionally tolerant: accepts fewer than three components ({@code "1.2"} = {@code "1.2.0"})
 * and ignores suffixes ({@code "1.2.0-SNAPSHOT"} = {@code "1.2.0"}) because the version comes from
 * an HTTP header supplied by a potentially outdated or misconfigured client.
 */
public final class SemanticVersion {

    private SemanticVersion() {}

    /**
     * Compares two versions.
     *
     * @return a negative value if {@code a} < {@code b}, zero if equal, or a positive value if {@code a} > {@code b}
     */
    public static int compare(String a, String b) {
        int[] left = parse(a);
        int[] right = parse(b);
        for (int i = 0; i < 3; i++) {
            int diff = Integer.compare(left[i], right[i]);
            if (diff != 0) return diff;
        }
        return 0;
    }

    /** Returns whether {@code version} predates {@code other}. */
    public static boolean isOlderThan(String version, String other) {
        return compare(version, other) < 0;
    }

    /**
     * Produces a version suitable for comparison. A missing or unreadable version becomes
     * {@code 0.0.0}; conservatively, an unidentified client is treated as the oldest possible
     * version rather than the newest.
     */
    private static int[] parse(String version) {
        int[] parts = {0, 0, 0};
        if (version == null || version.isBlank()) return parts;
        String cleaned = version.trim();
        int suffix = cleaned.indexOf('-');
        if (suffix >= 0) cleaned = cleaned.substring(0, suffix);
        String[] chunks = cleaned.split("\\.");
        for (int i = 0; i < 3 && i < chunks.length; i++) {
            try {
                parts[i] = Integer.parseInt(chunks[i].trim());
            } catch (NumberFormatException ex) {
                return new int[]{0, 0, 0}; // Treat an unreadable version as the oldest.
            }
        }
        return parts;
    }
}
