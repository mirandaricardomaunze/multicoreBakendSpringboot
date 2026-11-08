package mz.multicore.erp.architecture.validation;

import java.util.regex.Pattern;

/**
 * Canonical input sanitizer protecting against injection attacks, log pollution, unwanted control
 * characters, and path traversal. Preserves valid Portuguese diacritics and currency symbols.
 */
public final class InputSanitizer {

    // ASCII control characters (0x00-0x08, 0x0B, 0x0C, 0x0E-0x1F, 0x7F), except \t, \n, and \r
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    // Dangerous script patterns in free-text fields
    private static final Pattern SCRIPT_TAGS = Pattern.compile("(?i)<script.*?>.*?</script.*?>|<script.*?>|</script.*?>|javascript:|onload=|onerror=");

    // Characters that enable path traversal in file names
    private static final Pattern PATH_TRAVERSAL = Pattern.compile("[/\\\\:*?\"<>|]|\\.\\.");

    private InputSanitizer() {}

    /**
     * Removes invisible and potentially dangerous control characters, such as null byte \0, while
     * preserving standard line breaks and tabs.
     */
    public static String stripControlCharacters(String input) {
        if (input == null) return null;
        return CONTROL_CHARS.matcher(input).replaceAll("");
    }

    /**
     * Sanitizes and normalizes general text by removing control characters and trimming whitespace.
     */
    public static String sanitizeText(String input) {
        if (input == null) return null;
        String cleaned = stripControlCharacters(input).trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Sanitizes search terms for SQL and JPQL queries by removing control characters and disruptive
     * quotation marks, then truncating to the specified limit.
     */
    public static String sanitizeSearchQuery(String query, int maxLength) {
        if (query == null) return "";
        String cleaned = stripControlCharacters(query).trim();
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength).trim();
        }
        return cleaned;
    }

    /**
     * Sanitizes free-form remarks, notes, and descriptions by removing script tags and control
     * characters, then truncating to the maximum length.
     */
    public static String sanitizeNotes(String notes, int maxLength) {
        if (notes == null) return null;
        String cleaned = stripControlCharacters(notes);
        cleaned = SCRIPT_TAGS.matcher(cleaned).replaceAll("");
        cleaned = cleaned.trim();
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    /**
     * Prevents path traversal in file names used for downloads, backups, or exports by removing
     * traversal sequences, slashes, and characters invalid on Windows and Linux file systems.
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "documento";
        String cleaned = stripControlCharacters(fileName).trim();
        cleaned = PATH_TRAVERSAL.matcher(cleaned).replaceAll("_");
        // Collapse consecutive underscores.
        cleaned = cleaned.replaceAll("_+", "_");
        if (cleaned.isBlank() || "_".equals(cleaned)) {
            return "documento";
        }
        return cleaned;
    }
}
