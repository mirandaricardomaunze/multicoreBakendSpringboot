package mz.multicore.erp.modules.hr.model;

/** Estado de uma retenção da folha. Ver docs/RH_COMPLETO_SPEC.md §B5. */
public enum PayrollLiabilityStatus {

    POR_ENTREGAR("Por entregar"),
    ENTREGUE("Entregue");

    private final String label;

    PayrollLiabilityStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
