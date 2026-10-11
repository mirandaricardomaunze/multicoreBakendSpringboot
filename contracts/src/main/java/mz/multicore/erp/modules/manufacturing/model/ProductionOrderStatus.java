package mz.multicore.erp.modules.manufacturing.model;

/**
 * Estados da ordem de produção industrial / transformação.
 */
public enum ProductionOrderStatus {
    DRAFT("Rascunho"),
    PLANNED("Planeada"),
    IN_PROGRESS("Em Execução"),
    COMPLETED("Concluída"),
    CANCELLED("Cancelada");

    private final String label;

    ProductionOrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
