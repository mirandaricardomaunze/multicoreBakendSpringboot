package mz.multicore.erp.modules.company.model;

/** The eleven first-level administrative divisions of Mozambique. */
public enum MozambiqueProvince {
    MAPUTO_CITY("Maputo Cidade"),
    MAPUTO_PROVINCE("Maputo Província"),
    GAZA("Gaza"),
    INHAMBANE("Inhambane"),
    SOFALA("Sofala"),
    MANICA("Manica"),
    TETE("Tete"),
    ZAMBEZIA("Zambézia"),
    NAMPULA("Nampula"),
    CABO_DELGADO("Cabo Delgado"),
    NIASSA("Niassa");

    private final String label;

    MozambiqueProvince(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
