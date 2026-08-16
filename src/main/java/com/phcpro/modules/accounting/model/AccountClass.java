package com.phcpro.modules.accounting.model;

/**
 * Classes do <b>PGC-NIRF</b> (Plano Geral de Contabilidade de Moçambique). A classe é dada pelo
 * primeiro dígito do código da conta — é essa a convenção do plano, e é por ela que o balancete
 * e os mapas se organizam.
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
     * Natureza habitual da classe. É só o ponto de partida: contas como "Clientes" (classe 2,
     * devedora) e "Fornecedores" (classe 2, <b>credora</b>) vivem na mesma classe, pelo que a
     * natureza é gravada em cada conta e não derivada da classe na hora de calcular saldos.
     */
    public AccountNature defaultNature() {
        return defaultNature;
    }

    /** Classe a partir do código da conta ({@code "2101"} → {@link #TERCEIROS}). */
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
