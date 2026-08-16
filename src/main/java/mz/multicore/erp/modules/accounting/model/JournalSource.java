package mz.multicore.erp.modules.accounting.model;

/** De onde nasceu o lançamento. Distingue o que o sistema lançou do que o contabilista lançou. */
public enum JournalSource {

    /** Lançado à mão pelo contabilista (ajustes, amortizações, regularizações). */
    MANUAL("Manual"),

    /** Emissão de fatura (inclui venda de balcão, que é uma fatura real). */
    INVOICE("Fatura"),

    /** Recebimento de cliente (recibo ou liquidação em tesouraria). */
    RECEIPT("Recibo"),

    /** Compra a fornecedor. */
    PURCHASE("Compra"),

    /** Processamento salarial. */
    PAYROLL("Salários");

    private final String label;

    JournalSource(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Lançamento gerado pelo sistema — não deve ser editado à mão. */
    public boolean isAutomatic() {
        return this != MANUAL;
    }
}
