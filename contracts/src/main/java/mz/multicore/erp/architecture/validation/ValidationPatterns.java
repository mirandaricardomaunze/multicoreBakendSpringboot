package mz.multicore.erp.architecture.validation;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Canonical validation patterns shared by the backend and frontend.
 * Single source of truth for Multicore ERP data formats in Mozambique.
 */
public final class ValidationPatterns {

    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public static final String NUIT_REGEX = "^\\d{9}$";
    public static final Pattern NUIT_PATTERN = Pattern.compile(NUIT_REGEX);

    /** Mozambican mobile (Vodacom 84/85, Tmcel 82/83, Movitel 86/87) and landline (21-28) numbers. */
    public static final String PHONE_MZ_REGEX = "^(?:\\+?258[\\s-]?)?(8[2-7]\\d{7}|2[1-8]\\d{6})$";
    public static final Pattern PHONE_MZ_PATTERN = Pattern.compile(PHONE_MZ_REGEX);

    /** Mozambican identity card: 12 digits followed by one uppercase letter. */
    public static final String BI_MZ_REGEX = "^\\d{12}[A-Z]$";
    public static final Pattern BI_MZ_PATTERN = Pattern.compile(BI_MZ_REGEX);

    /** Barcode: EAN-13, EAN-8, UPC, or industrial-standard alphanumeric code (4 to 30 characters). */
    public static final String BARCODE_REGEX = "^[A-Za-z0-9\\-_]{4,30}$";
    public static final Pattern BARCODE_PATTERN = Pattern.compile(BARCODE_REGEX);

    /** Item code (SKU): clean alphanumeric value (2 to 30 characters). */
    public static final String SKU_REGEX = "^[A-Za-z0-9\\-_]{2,30}$";
    public static final Pattern SKU_PATTERN = Pattern.compile(SKU_REGEX);

    private ValidationPatterns() {}

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        return !trimmed.isBlank() && EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Canonical Mozambican NUIT validation: nine digits with a Modulus 11 check digit.
     * NUIT 999999999 is accepted as the canonical final-consumer code.
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

    /** Normalizes a Mozambican telephone number to national format (for example, "+258 84 123 4567" -> "841234567"). */
    public static String cleanPhoneMozambique(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("258") && digits.length() == 12) {
            return digits.substring(3);
        }
        return digits;
    }
}
