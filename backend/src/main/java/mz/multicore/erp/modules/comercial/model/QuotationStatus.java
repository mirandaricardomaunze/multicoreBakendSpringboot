package mz.multicore.erp.modules.comercial.model;

/**
 * Estados da cotação. Repare-se no que <b>não</b> está aqui: {@code EXPIRED}.
 *
 * <p>A caducidade não é um estado gravado — é derivada de {@code validUntil} contra a data de hoje
 * ({@link Quotation#isExpired}). Gravá-la obrigaria a um agendador a passear pela tabela todas as
 * noites, deixaria linhas desactualizadas entre passagens, e estender a validade exigiria uma
 * dança de estados para voltar atrás. Ver docs/COTACAO_SPEC.md §4.
 */
public enum QuotationStatus {

    DRAFT("Rascunho"),
    SENT("Enviada"),
    ACCEPTED("Aceite"),
    REJECTED("Recusada"),
    CONVERTED("Convertida"),
    CANCELLED("Cancelada");

    private final String label;

    QuotationStatus(String label) {
        this.label = label;
    }

    /** Rótulo em PT-MZ. O operador nunca vê o nome da constante. */
    public String getLabel() {
        return label;
    }

    /**
     * A proposta ainda está viva — pode ser trabalhada e, se não tiver caducado, convertida.
     * <b>Fonte única</b> desta pergunta: nem o painel nem o controller enumeram estados.
     */
    public boolean isOpen() {
        return this == DRAFT || this == SENT || this == ACCEPTED;
    }

    /** Nada mais acontece a esta cotação. */
    public boolean isTerminal() {
        return !isOpen();
    }
}
