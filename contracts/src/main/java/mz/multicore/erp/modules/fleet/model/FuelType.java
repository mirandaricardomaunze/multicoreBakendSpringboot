package mz.multicore.erp.modules.fleet.model;

/**
 * Tipo de combustível / propulsão da viatura.
 */
public enum FuelType {
    DIESEL("Gasóleo (Diesel)"),
    PETROL("Gasolina"),
    HYBRID("Híbrido"),
    ELECTRIC("Elétrico");

    private final String label;

    FuelType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
