package mz.multicore.erp.modules.accounting.model;

/**
 * Origin of a cost centre (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1): one per warehouse/store, kept in step with
 * that warehouse, or created by hand by a manager (Administração, Frota, Produção...).
 */
public enum CostCenterType {
    WAREHOUSE("Armazém"),
    MANUAL("Manual");

    private final String label;

    CostCenterType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
