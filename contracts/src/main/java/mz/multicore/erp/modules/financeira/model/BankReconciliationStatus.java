package mz.multicore.erp.modules.financeira.model;

/**
 * Estado do processo de reconciliação de um extracto bancário.
 */
public enum BankReconciliationStatus {
    OPEN("Em Aberto"),
    RECONCILED("Conciliado"),
    CLOSED("Fechado");

    private final String label;

    BankReconciliationStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
