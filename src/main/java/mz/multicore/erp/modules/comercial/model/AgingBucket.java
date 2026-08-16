package mz.multicore.erp.modules.comercial.model;

/**
 * Escalão de antiguidade de uma dívida — a pergunta "há quanto tempo é que isto está por receber?".
 *
 * <p><b>Fonte única</b> dos cortes: quem quiser classificar uma dívida chama {@link #of(int)}.
 * O dashboard, as contas correntes e o mapa de antiguidade partilham esta escala — a lição do IVA
 * e do saldo em dívida (a mesma regra em duas portas diverge em silêncio).
 *
 * <p>Os cortes (30/60/90 dias) são a convenção comercial corrente em Moçambique. Uma factura
 * dentro do prazo é {@link #CORRENTE} — não está em atraso, apenas por receber.
 */
public enum AgingBucket {

    CORRENTE("Corrente (por vencer)"),
    ATE_30("1–30 dias"),
    DE_31_A_60("31–60 dias"),
    DE_61_A_90("61–90 dias"),
    MAIS_DE_90("Mais de 90 dias");

    private final String label;

    AgingBucket(String label) {
        this.label = label;
    }

    /** Rótulo em PT-MZ para tabelas e relatórios. */
    public String label() {
        return label;
    }

    /** A dívida já passou do vencimento (tudo menos {@link #CORRENTE}). */
    public boolean isOverdue() {
        return this != CORRENTE;
    }

    /**
     * Classifica pelos dias de atraso. Zero ou negativo (ainda dentro do prazo) é
     * {@link #CORRENTE}; o primeiro dia de atraso já cai em {@link #ATE_30}.
     */
    public static AgingBucket of(int daysOverdue) {
        if (daysOverdue <= 0)  return CORRENTE;
        if (daysOverdue <= 30) return ATE_30;
        if (daysOverdue <= 60) return DE_31_A_60;
        if (daysOverdue <= 90) return DE_61_A_90;
        return MAIS_DE_90;
    }
}
