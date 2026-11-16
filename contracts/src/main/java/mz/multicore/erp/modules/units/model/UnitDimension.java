package mz.multicore.erp.modules.units.model;

/**
 * Dimension of a unit of measure (SA-C02). Conversion is fixed within a dimension (1 t = 1000 kg).
 * {@link #PACKAGE} has no universal conversion: a bag of cement and a bag of rice weigh differently, so the
 * factor is always defined on the product.
 */
public enum UnitDimension {
    COUNT("Contagem", "UN"),
    MASS("Massa", "KG"),
    VOLUME("Volume", "L"),
    LENGTH("Comprimento", "M"),
    AREA("Área", "M2"),
    PACKAGE("Embalagem", null);

    private final String label;
    private final String referenceCode;

    UnitDimension(String label, String referenceCode) {
        this.label = label;
        this.referenceCode = referenceCode;
    }

    public String label() {
        return label;
    }

    /** Code of the dimension reference unit, or {@code null} for packages. */
    public String referenceCode() {
        return referenceCode;
    }

    public boolean hasFixedConversion() {
        return referenceCode != null;
    }
}
