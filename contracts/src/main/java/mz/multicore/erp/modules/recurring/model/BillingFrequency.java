package mz.multicore.erp.modules.recurring.model;

/**
 * Frequência / periodicidade de emissão da facturação recorrente.
 */
public enum BillingFrequency {
    MONTHLY("Mensal", 1),
    QUARTERLY("Trimestral", 3),
    BIANNUAL("Semestral", 6),
    ANNUAL("Anual", 12);

    private final String label;
    private final int monthsInterval;

    BillingFrequency(String label, int monthsInterval) {
        this.label = label;
        this.monthsInterval = monthsInterval;
    }

    public String label() {
        return label;
    }

    public int monthsInterval() {
        return monthsInterval;
    }
}
