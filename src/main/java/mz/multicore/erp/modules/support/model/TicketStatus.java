package mz.multicore.erp.modules.support.model;

/** Estado de um pedido de assistência à plataforma. */
public enum TicketStatus {
    OPEN("Aberto"),
    IN_PROGRESS("Em curso"),
    RESOLVED("Resolvido"),
    CLOSED("Fechado");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
