package mz.multicore.erp.modules.inventory.model;

/**
 * Estado do registo de quebra / desperdício de stock.
 */
public enum WasteStatus {
    /** Pendente de validação e aprovação de gerência (quando ultrapassa a alçada). */
    PENDING_APPROVAL("Pendente Aprovação"),
    /** Aprovada e com abate efetivo realizado no stock físico e contabilidade. */
    APPROVED("Aprovado"),
    /** Rejeitada pela gerência (não abate stock). */
    REJECTED("Rejeitado");

    private final String description;

    WasteStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
