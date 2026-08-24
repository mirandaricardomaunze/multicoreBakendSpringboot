package mz.multicore.erp.modules.hr.model;

/**
 * Estados do contrato de trabalho. Repare-se no que <b>não</b> está aqui: {@code EXPIRADO}.
 *
 * <p>A caducidade não é um estado gravado — é derivada da data de fim contra hoje
 * ({@link EmploymentContract#isExpired}). Mesma lição da cotação
 * ({@code QuotationStatus}): gravá-la obrigaria a um agendador nocturno a passear pela tabela,
 * deixaria linhas desactualizadas entre passagens, e prolongar um contrato exigiria uma dança de
 * estados para voltar atrás. Ver docs/RH_COMPLETO_SPEC.md §B1.
 */
public enum ContractStatus {

    RASCUNHO("Rascunho"),
    VIGENTE("Vigente"),
    CESSADO("Cessado");

    private final String label;

    ContractStatus(String label) {
        this.label = label;
    }

    /** Rótulo em PT-MZ. O operador nunca vê o nome da constante. */
    public String getLabel() {
        return label;
    }

    /** Nada mais acontece a este contrato. */
    public boolean isTerminal() {
        return this == CESSADO;
    }
}
