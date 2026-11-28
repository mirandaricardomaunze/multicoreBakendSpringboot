package mz.multicore.erp.modules.comercial.model;

/**
 * Estado da entrega de uma guia de remessa, separado do estado da guia
 * (docs/ENTREGAS_AGENDA_PROVA_SPEC.md §3.1).
 */
public enum DeliveryStatus {

    UNSCHEDULED("Por agendar"),
    SCHEDULED("Agendada"),
    DELIVERED("Entregue"),
    NOT_DELIVERED("Não entregue");

    private final String label;

    DeliveryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
