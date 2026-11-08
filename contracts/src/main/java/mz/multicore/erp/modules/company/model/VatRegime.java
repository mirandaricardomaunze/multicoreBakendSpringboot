package mz.multicore.erp.modules.company.model;

/** VAT regime the company is registered under at the tax authority (AT-MZ). */
public enum VatRegime {
    NORMAL("Regime normal"),
    SIMPLIFIED("Regime simplificado"),
    EXEMPT("Isento de IVA"),
    ISPC("ISPC (pequenos contribuintes)");

    private final String label;

    VatRegime(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
