package mz.multicore.erp.modules.hr.model;

/** Estado do acerto final. Ver docs/RH_COMPLETO_SPEC.md §B3. */
public enum SettlementStatus {

    POR_PAGAR("Por pagar"),
    PAGO("Pago");

    private final String label;

    SettlementStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
