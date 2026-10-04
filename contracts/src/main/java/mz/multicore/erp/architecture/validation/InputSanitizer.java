package mz.multicore.erp.architecture.validation;

import java.util.regex.Pattern;

/**
 * Sanitizador canónico de entradas para proteção contra ataques de injeção,
 * poluição de logs, caracteres de controlo indesejados e Path Traversal.
 * Não altera acentuação legítima da língua portuguesa nem símbolos monetários.
 */
public final class InputSanitizer {

    // Caracteres de controlo ASCII (0x00-0x08, 0x0B, 0x0C, 0x0E-0x1F, 0x7F) excepto \t, \n, \r
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    // Padrões de scripts perigosos para campos de texto livre
    private static final Pattern SCRIPT_TAGS = Pattern.compile("(?i)<script.*?>.*?</script.*?>|<script.*?>|</script.*?>|javascript:|onload=|onerror=");

    // Caracteres perigosos para Path Traversal em nomes de ficheiro
    private static final Pattern PATH_TRAVERSAL = Pattern.compile("[/\\\\:*?\"<>|]|\\.\\.");

    private InputSanitizer() {}

    /**
     * Remove caracteres de controlo invisíveis e potencialmente perigosos (ex.: byte nulo \0),
     * preservando quebras de linha e tabulações normais.
     */
    public static String stripControlCharacters(String input) {
        if (input == null) return null;
        return CONTROL_CHARS.matcher(input).replaceAll("");
    }

    /**
     * Sanitiza e normaliza um texto genérico: remove caracteres de controlo e faz trim.
     */
    public static String sanitizeText(String input) {
        if (input == null) return null;
        String cleaned = stripControlCharacters(input).trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Sanitiza termos de pesquisa para queries SQL/JPQL:
     * remove caracteres de controlo, remove aspas disruptivas e trunca ao limite especificado.
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
     * Sanitiza campos de observações, notas e descrições livres:
     * remove tags de script, limpa caracteres de controlo e trunca ao tamanho máximo.
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
     * Previne Path Traversal em nomes de ficheiro (ex.: downloads, backups ou exportações):
     * remove ../, .., barras e caracteres inválidos para sistemas de ficheiros no Windows e Linux.
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "documento";
        String cleaned = stripControlCharacters(fileName).trim();
        cleaned = PATH_TRAVERSAL.matcher(cleaned).replaceAll("_");
        // Remove múltiplos underscores consecutivos
        cleaned = cleaned.replaceAll("_+", "_");
        if (cleaned.isBlank() || "_".equals(cleaned)) {
            return "documento";
        }
        return cleaned;
    }
}
