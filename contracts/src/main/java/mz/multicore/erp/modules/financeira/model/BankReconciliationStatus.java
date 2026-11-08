package mz.multicore.erp.modules.financeira.model;

/**
 * Status of the bank-statement reconciliation process.
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
