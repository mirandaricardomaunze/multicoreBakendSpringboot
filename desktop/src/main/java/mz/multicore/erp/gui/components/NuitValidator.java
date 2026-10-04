package mz.multicore.erp.gui.components;

/**
 * Validador canónico de NUIT (Número Único de Identificação Tributária) de Moçambique.
 * Regras da Autoridade Tributária (AT):
 * - Exatamente 9 dígitos numéricos.
 * - Rejeita sequências repetidas (ex.: 000000000, 111111111).
 * - Suporta 999999999 como Consumidor Final genérico.
 * - Verificação de dígito de controlo por Módulo 11 nos primeiros 8 dígitos.
 */
public final class NuitValidator {

    public static final String FINAL_CONSUMER_NUIT = "999999999";

    private NuitValidator() {}

    /**
     * Valida se a string representa um NUIT moçambicano válido.
     *
     * @param rawNUIT número do NUIT com ou sem espaços
     * @return true se válido; false caso contrário
     */
    public static boolean isValid(String rawNUIT) {
        if (rawNUIT == null) return false;
        String clean = rawNUIT.trim().replace(" ", "").replace("-", "");
        if (clean.length() != 9 || !clean.matches("^\\d{9}$")) return false;

        // Consumidor final padrão é aceite
        if (FINAL_CONSUMER_NUIT.equals(clean)) return true;

        // Rejeita sequências com todos os dígitos iguais
        char first = clean.charAt(0);
        boolean allSame = true;
        for (int i = 1; i < 9; i++) {
            if (clean.charAt(i) != first) {
                allSame = false;
                break;
            }
        }
        if (allSame) return false;

        // Cálculo de dígito de controlo Módulo 11
        // Pesos: 9, 8, 7, 6, 5, 4, 3, 2 aplicados aos primeiros 8 dígitos
        int sum = 0;
        int[] weights = {9, 8, 7, 6, 5, 4, 3, 2};
        for (int i = 0; i < 8; i++) {
            int digit = clean.charAt(i) - '0';
            sum += digit * weights[i];
        }

        int remainder = sum % 11;
        int checkDigit = (11 - remainder) % 11;
        if (checkDigit == 10) checkDigit = 0;

        int expectedLastDigit = clean.charAt(8) - '0';
        return checkDigit == expectedLastDigit;
    }

    /**
     * Gera o dígito de controlo para os primeiros 8 dígitos de um NUIT moçambicano.
     */
    public static int calculateCheckDigit(String first8Digits) {
        if (first8Digits == null || first8Digits.length() != 8 || !first8Digits.matches("^\\d{8}$")) {
            throw new IllegalArgumentException("Necessários exatamente 8 dígitos numéricos");
        }
        int sum = 0;
        int[] weights = {9, 8, 7, 6, 5, 4, 3, 2};
        for (int i = 0; i < 8; i++) {
            sum += (first8Digits.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        int check = (11 - remainder) % 11;
        return check == 10 ? 0 : check;
    }
}
