package mz.multicore.erp.modules.pickup.model;

/** Lifecycle of a paid-material pickup account (SA-C07). */
public enum PickupAccountStatus {
    OPEN("Por levantar"),
    COMPLETED("Levantado"),
    CANCELLED("Anulado");

    private final String label;

    PickupAccountStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
