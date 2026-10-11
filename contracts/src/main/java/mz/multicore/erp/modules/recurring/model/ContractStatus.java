package mz.multicore.erp.modules.recurring.model;

/**
 * Estados do contrato de facturação recorrente / avença.
 */
public enum ContractStatus {
    ACTIVE("Activo"),
    SUSPENDED("Suspenso"),
    EXPIRED("Expirado"),
    CANCELLED("Cancelado");

    private final String label;

    ContractStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
