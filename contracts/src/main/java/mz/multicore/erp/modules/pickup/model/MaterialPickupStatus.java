package mz.multicore.erp.modules.pickup.model;

/** Status of a single pickup slip (LV series). Cancelled slips return their quantities to the balance. */
public enum MaterialPickupStatus {
    ISSUED("Emitido"),
    CANCELLED("Anulado");

    private final String label;

    MaterialPickupStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
