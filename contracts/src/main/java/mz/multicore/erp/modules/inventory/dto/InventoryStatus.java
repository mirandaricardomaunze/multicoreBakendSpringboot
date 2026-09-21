package mz.multicore.erp.modules.inventory.dto;

/**
 * Estados da sessão de inventário físico.
 */
public enum InventoryStatus {
    DRAFT("Rascunho"),
    IN_PROGRESS("Em Contagem"),
    CLOSED("Concluído"),
    CANCELLED("Cancelado");

    private final String description;

    InventoryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
