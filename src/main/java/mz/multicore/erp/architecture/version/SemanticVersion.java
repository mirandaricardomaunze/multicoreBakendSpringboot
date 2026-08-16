package mz.multicore.erp.architecture.version;

/**
 * Comparação de versões {@code MAIOR.MENOR.CORRECÇÃO} (ex.: {@code 1.4.2}).
 *
 * <p><b>Fonte única</b> da pergunta "esta versão é mais antiga do que aquela?". Comparar versões
 * como texto é o erro clássico — {@code "1.10.0" < "1.9.0"} em ordem alfabética, e o resultado é
 * um cliente novo a ser bloqueado como se fosse velho. Aqui compara-se número a número.
 *
 * <p>Tolerante de propósito: aceita menos de três partes ({@code "1.2"} = {@code "1.2.0"}) e
 * ignora sufixos ({@code "1.2.0-SNAPSHOT"} = {@code "1.2.0"}), porque a versão chega de um
 * cabeçalho HTTP escrito por um cliente que pode estar desactualizado ou mal configurado.
 */
public final class SemanticVersion {

    private SemanticVersion() {}

    /**
     * Compara duas versões.
     *
     * @return negativo se {@code a} < {@code b}, zero se iguais, positivo se {@code a} > {@code b}
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

    /** {@code version} é anterior a {@code other}. */
    public static boolean isOlderThan(String version, String other) {
        return compare(version, other) < 0;
    }

    /**
     * Versão utilizável para comparação. Uma versão ilegível ou ausente vale {@code 0.0.0} — a
     * leitura conservadora: um cliente que não se identifica é tratado como o mais antigo
     * possível, e não como o mais recente.
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
                return new int[]{0, 0, 0}; // versão ilegível: trata como a mais antiga
            }
        }
        return parts;
    }
}
