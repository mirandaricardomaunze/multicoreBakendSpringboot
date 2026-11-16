package mz.multicore.erp.modules.advances;

/**
 * Shared constants of customer advances (SA-C10, {@code docs/ADIANTAMENTOS_SPEC.md}). Framework-free so the
 * backend, the desktop and the tests agree on how the system product and the offset receipt are recognised.
 */
public final class AdvancePayments {

    /** SKU prefix of the per-company system product "Adiantamento" (SKUs are unique across companies). */
    public static final String SYSTEM_SKU_PREFIX = "SYS-ADIANT-";

    /** Payment method of the receipt that moves an advance onto the final invoice. It is not money. */
    public static final String OFFSET_PAYMENT_METHOD = "COMPENSACAO_ADIANTAMENTO";

    private AdvancePayments() {}

    public static String systemSku(Long companyId) {
        return SYSTEM_SKU_PREFIX + companyId;
    }

    public static boolean isSystemSku(String sku) {
        return sku != null && sku.startsWith(SYSTEM_SKU_PREFIX);
    }

    public static boolean isOffsetPayment(String paymentMethod) {
        return OFFSET_PAYMENT_METHOD.equals(paymentMethod);
    }
}
