package mz.multicore.erp.modules.crm.model;

/** Urgência de um pedido de assistência — decide a ordem por que a equipa técnica pega no trabalho. */
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
