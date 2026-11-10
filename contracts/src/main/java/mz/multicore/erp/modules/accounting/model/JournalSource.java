package mz.multicore.erp.modules.accounting.model;

/** Origin of a journal entry, distinguishing system-generated entries from accountant entries. */
public enum JournalSource {

    /** Manually posted by the accountant for adjustments, amortization, or regularization. */
    MANUAL("Manual"),

    /** Invoice issuance, including counter sales, which are actual invoices. */
    INVOICE("Fatura"),
    INVOICE_CANCELLATION("Anulação de fatura"),
    RECEIPT_CANCELLATION("Anulação de recibo"),

    /** Customer receipt or treasury settlement. */
    RECEIPT("Recibo"),

    /** Purchase from a supplier. */
    PURCHASE("Compra"),

    /** Payment settling a supplier purchase. */
    SUPPLIER_PAYMENT("Pagamento a fornecedor"),

    /** Approved credit note. */
    CREDIT_NOTE("Nota de crédito"),

    /** Approved debit note. */
    DEBIT_NOTE("Nota de débito"),

    /** Payroll processing. */
    PAYROLL("Salários"),

    /**
     * Remittance of payroll withholdings to the State (IRPS, INSS). Uses a distinct source rather
     * than {@link #PAYROLL} because the deduplication key is <b>source + document ID</b>. With the
     * same source, payslip 12 and withholding 12 would appear to be the same entry.
     */
    PAYROLL_DELIVERY("Entrega de retenções"),

    /** Production-order completion, including finished-goods receipt and production variance. */
    PRODUCTION("Produção industrial"),

    /** Periodic amortization or depreciation charge for a fixed asset. */
    ASSET_DEPRECIATION("Depreciação de activo"),

    /** Fuel supplied to a fleet vehicle. */
    FLEET_FUEL("Combustível de frota"),
    STOCK_WASTE("Quebra de existências"),
    STOCK_ADJUSTMENT("Acerto de existências"),
    CARCASS_BREAKDOWN("Desmancha"),
    CARCASS_BREAKDOWN_CANCEL("Anulação de desmancha"),
    PRODUCTION_RUN("Lote produzido"),
    PRODUCTION_CONSUMPTION("Consumo de produção");

    private final String label;

    JournalSource(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** System-generated entry that must not be edited manually. */
    public boolean isAutomatic() {
        return this != MANUAL;
    }
}
