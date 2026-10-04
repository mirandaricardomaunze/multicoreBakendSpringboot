package mz.multicore.erp.modules.pos.dto;

/**
 * Operadores móveis suportados para pagamentos integrados em Moçambique.
 */
public enum MobilePaymentProvider {
    MPESA("M-Pesa", "Vodacom", "84, 85"),
    EMOLA("e-Mola", "Movitel", "86, 87");

    private final String displayName;
    private final String operator;
    private final String prefixes;

    MobilePaymentProvider(String displayName, String operator, String prefixes) {
        this.displayName = displayName;
        this.operator = operator;
        this.prefixes = prefixes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getOperator() {
        return operator;
    }

    public String getPrefixes() {
        return prefixes;
    }
}
