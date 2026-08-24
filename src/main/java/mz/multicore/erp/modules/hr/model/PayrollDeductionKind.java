package mz.multicore.erp.modules.hr.model;

/**
 * Os três casos que o campo único {@code otherDeductions} escondia.
 * Ver docs/RH_COMPLETO_SPEC.md §B6.
 *
 * <p>Partilham tabela porque partilham forma: um empréstimo é um adiantamento em N prestações, e um
 * desconto recorrente é um empréstimo sem capital. O que os distingue é <b>se o dinheiro saiu da
 * caixa antes</b> e <b>quantas vezes se desconta</b>.
 */
public enum PayrollDeductionKind {

    /** Dinheiro entregue a meio do mês, descontado por inteiro no recibo do período. */
    ADIANTAMENTO("Adiantamento", true),

    /** Capital entregue, devolvido em prestações ao longo de vários recibos. */
    EMPRESTIMO("Empréstimo", true),

    /** Sindicato, seguro, quota — desconta enquanto vigorar, e não devolve capital nenhum. */
    RECORRENTE("Desconto recorrente", false);

    private final String label;
    private final boolean paysOutOnCreation;

    PayrollDeductionKind(String label, boolean paysOutOnCreation) {
        this.label = label;
        this.paysOutOnCreation = paysOutOnCreation;
    }

    public String getLabel() {
        return label;
    }

    /** Criar isto entrega dinheiro ao colaborador — logo, sai da tesouraria na hora. */
    public boolean paysOutOnCreation() {
        return paysOutOnCreation;
    }
}
