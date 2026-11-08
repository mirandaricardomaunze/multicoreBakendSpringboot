package mz.multicore.erp.modules.accounting.model;

/**
 * Classes in the <b>PGC-NIRF</b>, Mozambique's General Accounting Plan. The first digit of the
 * account code determines the class, following the convention used to organize trial balances
 * and financial statements.
 */
public enum AccountClass {

    MEIOS_CIRCULANTES_FINANCEIROS(1, "Meios circulantes financeiros", AccountNature.DEVEDORA),
    TERCEIROS(2, "Terceiros", AccountNature.DEVEDORA),
    EXISTENCIAS(3, "Existências", AccountNature.DEVEDORA),
    IMOBILIZADO(4, "Imobilizado", AccountNature.DEVEDORA),
    CAPITAL_E_RESERVAS(5, "Capital, reservas e resultados transitados", AccountNature.CREDORA),
    CUSTOS_E_PERDAS(6, "Custos e perdas", AccountNature.DEVEDORA),
    PROVEITOS_E_GANHOS(7, "Proveitos e ganhos", AccountNature.CREDORA),
    RESULTADOS(8, "Resultados", AccountNature.CREDORA);

    private final int digit;
    private final String label;
    private final AccountNature defaultNature;

    AccountClass(int digit, String label, AccountNature defaultNature) {
        this.digit = digit;
        this.label = label;
        this.defaultNature = defaultNature;
    }

    public int digit() {
        return digit;
    }

    public String label() {
        return label;
    }

    /**
     * Typical nature of the class. This is only a default: accounts such as Customers (class 2,
     * debit) and Suppliers (class 2, <b>credit</b>) share a class. Each account therefore stores its
     * nature instead of deriving it from the class when balances are calculated.
     */
    public AccountNature defaultNature() {
        return defaultNature;
    }

    /** Resolves the class from an account code ({@code "2101"} → {@link #TERCEIROS}). */
    public static AccountClass ofCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Código de conta vazio.");
        }
        char first = code.trim().charAt(0);
        for (AccountClass value : values()) {
            if (value.digit == Character.getNumericValue(first)) return value;
        }
        throw new IllegalArgumentException("Código de conta fora das classes do PGC-NIRF: " + code);
    }
}
