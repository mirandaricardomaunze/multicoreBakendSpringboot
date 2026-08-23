package mz.multicore.erp.modules.crm.model;

/**
 * Estado de um pedido de assistência ao cliente da loja.
 *
 * <p>Antes era uma {@code String} livre com dois valores escritos à mão ("OPEN"/"RESOLVED") em
 * pontos diferentes do {@code CRMService}. Os nomes das constantes mantêm-se iguais a essas strings
 * de propósito — os pedidos já gravados continuam a ler-se sem migração de dados.
 */
public enum TicketStatus {
    OPEN("Aberto"),
    IN_PROGRESS("Em curso"),
    RESOLVED("Resolvido"),
    CANCELLED("Anulado");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Um pedido fechado (resolvido ou anulado) não recebe trabalho novo sem ser reaberto. */
    public boolean isTerminal() {
        return this == RESOLVED || this == CANCELLED;
    }
}
