package mz.multicore.erp.gui;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class SubscriptionRenewalPricingCalculator {

    private SubscriptionRenewalPricingCalculator() {}

    static String planKeyForIndex(int idx) {
        return switch (idx) {
            case 1 -> "BASIC";
            case 2 -> "ENTERPRISE";
            default -> "PRO";
        };
    }

    static int monthsForIndex(int idx) {
        return switch (idx) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 12;
            default -> 1;
        };
    }

    static BigDecimal calculateTotal(String planKey, int months) {
        BigDecimal monthly = switch (planKey) {
            case "BASIC" -> new BigDecimal("1500.00");
            case "PRO" -> new BigDecimal("3500.00");
            case "ENTERPRISE" -> new BigDecimal("7500.00");
            default -> new BigDecimal("3500.00");
        };
        BigDecimal total = monthly.multiply(BigDecimal.valueOf(months));
        if (months >= 12) {
            total = total.multiply(new BigDecimal("0.85"));
        } else if (months >= 6) {
            total = total.multiply(new BigDecimal("0.90"));
        } else if (months >= 3) {
            total = total.multiply(new BigDecimal("0.95"));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    static String discountLabelForMonths(int months) {
        if (months >= 12) return "(15% de Desconto Anual Aplicado)";
        if (months >= 6) return "(10% de Desconto Semestral)";
        if (months >= 3) return "(5% de Desconto Trimestral)";
        return "";
    }

    static BigDecimal parseAmountFromLabel(String text) {
        try {
            String clean = text.replaceAll("[^0-9,.]", "").replace(".", "").replace(",", ".");
            return new BigDecimal(clean);
        } catch (Exception ex) {
            return new BigDecimal("3500.00");
        }
    }
}
