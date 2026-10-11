package mz.multicore.erp.modules.inventory.model;

/**
 * Ciclo de vida de uma guia de transferência. A guia nasce {@link #DRAFT};
 * o stock só sai do armazém de origem quando passa a {@link #APPROVED}. {@link #REJECTED}
 * e {@link #CANCELLED} encerram a guia sem qualquer movimento de stock.
 */
public enum TransferStatus {
    DRAFT("Rascunho"),
    AWAITING_SEPARATION("Aguarda Separação"),
    IN_SEPARATION("Em Separação"),
    SEPARATED("Separada"),
    PENDING_APPROVAL("Pendente de Aprovação"),
    APPROVED("Aprovada"),
    REJECTED("Rejeitada"),
    CANCELLED("Cancelada");

    private final String label;

    TransferStatus(String label) {
        this.label = label;
    }

    /** Rótulo legível (PT-MZ) para apresentação em UI/PDF. */
    public String getLabel() {
        return label;
    }

    public boolean canEdit() {
        return this == DRAFT;
    }

    public boolean isSeparatedOrPending() {
        return this == SEPARATED || this == PENDING_APPROVAL;
    }
}
