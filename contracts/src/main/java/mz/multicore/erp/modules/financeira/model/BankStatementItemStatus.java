package mz.multicore.erp.modules.financeira.model;

/**
 * Estado de conciliação de uma linha individual do extracto bancário.
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
