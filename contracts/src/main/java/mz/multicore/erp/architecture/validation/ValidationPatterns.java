package mz.multicore.erp.architecture.validation;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Padrões de validação canónicos partilhados entre backend e frontend.
 * Fonte única de verdade para formatos de dados do ERP Multicore em Moçambique.
 */
public final class ValidationPatterns {

    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public static final String NUIT_REGEX = "^\\d{9}$";
    public static final Pattern NUIT_PATTERN = Pattern.compile(NUIT_REGEX);

    /** Telefones móveis (Vodacom 84/85, Tmcel 82/83, Movitel 86/87) e fixos (21-28) em Moçambique. */
    public static final String PHONE_MZ_REGEX = "^(?:\\+?258[\\s-]?)?(8[2-7]\\d{7}|2[1-8]\\d{6})$";
    public static final Pattern PHONE_MZ_PATTERN = Pattern.compile(PHONE_MZ_REGEX);

    /** Bilhete de Identidade Moçambicano: 12 algarismos seguidos de 1 letra maiúscula. */
    public static final String BI_MZ_REGEX = "^\\d{12}[A-Z]$";
    public static final Pattern BI_MZ_PATTERN = Pattern.compile(BI_MZ_REGEX);

    /** Código de barras: EAN-13, EAN-8, UPC ou alfanumérico padrão industrial (4 a 30 caracteres). */
    public static final String BARCODE_REGEX = "^[A-Za-z0-9\\-_]{4,30}$";
    public static final Pattern BARCODE_PATTERN = Pattern.compile(BARCODE_REGEX);

    /** Código de artigo (SKU): alfanumérico limpo (2 a 30 caracteres). */
    public static final String SKU_REGEX = "^[A-Za-z0-9\\-_]{2,30}$";
    public static final Pattern SKU_PATTERN = Pattern.compile(SKU_REGEX);

    private ValidationPatterns() {}

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        return !trimmed.isBlank() && EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Validação canónica de NUIT em Moçambique (9 dígitos com algoritmo de dígito de controlo Módulo 11).
     * O NUIT 999999999 é aceite como código canónico de Consumidor Final.
     */
    public static boolean isValidNuit(String nuit) {
        if (nuit == null) return false;
        String trimmed = nuit.trim().replace(" ", "").replace("-", "");
        if (!NUIT_PATTERN.matcher(trimmed).matches()) return false;
        if ("999999999".equals(trimmed)) return true; // Consumidor Final

        int[] weights = {9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += Character.digit(trimmed.charAt(i), 10) * weights[i];
        }

        int remainder = sum % 11;
        int checkDigit = remainder < 2 ? 0 : 11 - remainder;
        return Character.digit(trimmed.charAt(8), 10) == checkDigit;
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String clean = phone.trim().replaceAll("[\\s-]", "");
        return PHONE_MZ_PATTERN.matcher(clean).matches();
    }

    public static boolean isValidBi(String bi) {
        if (bi == null) return false;
        String clean = bi.trim().toUpperCase();
        return BI_MZ_PATTERN.matcher(clean).matches();
    }

    public static boolean isValidBarcode(String barcode) {
        if (barcode == null) return false;
        String clean = barcode.trim();
        return BARCODE_PATTERN.matcher(clean).matches();
    }

    public static boolean isValidSku(String sku) {
        if (sku == null) return false;
        String clean = sku.trim();
        return SKU_PATTERN.matcher(clean).matches();
    }

    public static boolean isValidPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isValidNonNegativeAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }

    public static boolean isValidPercentage(BigDecimal pct) {
        return pct != null && pct.compareTo(BigDecimal.ZERO) >= 0 && pct.compareTo(BigDecimal.valueOf(100)) <= 0;
    }

    /** Limpa telefone moçambicano para o formato nacional padrão (ex.: "+258 84 123 4567" -> "841234567"). */
    public static String cleanPhoneMozambique(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("258") && digits.length() == 12) {
            return digits.substring(3);
        }
        return digits;
    }
}
