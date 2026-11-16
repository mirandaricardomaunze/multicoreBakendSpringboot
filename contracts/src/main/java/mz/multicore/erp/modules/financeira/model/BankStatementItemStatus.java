package mz.multicore.erp.modules.financeira.model;

/**
 * Reconciliation status of an individual bank-statement line.
 */
public enum BankStatementItemStatus {
    UNMATCHED("Pendente"),
    MATCHED("Conciliado"),
    IGNORED("Ignorado");

    private final String label;

    BankStatementItemStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
