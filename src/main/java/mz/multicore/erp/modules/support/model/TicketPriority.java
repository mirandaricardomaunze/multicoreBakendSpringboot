package mz.multicore.erp.modules.support.model;

/** Prioridade de um pedido de assistência. */
public enum TicketPriority {
    LOW("Baixa"),
    NORMAL("Normal"),
    HIGH("Alta"),
    URGENT("Urgente");

    private final String label;

    TicketPriority(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
