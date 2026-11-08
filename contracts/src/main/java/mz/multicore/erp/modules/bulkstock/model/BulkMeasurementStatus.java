package mz.multicore.erp.modules.bulkstock.model;

/** Outcome of a bulk measurement. Differences above the tolerance wait for a manager. */
public enum BulkMeasurementStatus {
    NO_DIFFERENCE("Sem diferença"),
    APPLIED("Acertado"),
    PENDING_APPROVAL("Aguarda aprovação"),
    REJECTED("Rejeitado");

    private final String label;

    BulkMeasurementStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
