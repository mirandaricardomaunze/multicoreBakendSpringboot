package mz.multicore.erp.modules.manufacturing.model;

/**
 * Tipo da ficha técnica (BOM).
 */
public enum BomType {
    STANDARD("Produção Padrão"),
    DISASSEMBLY("Desmontagem"),
    KIT("Kit Comercial");

    private final String label;

    BomType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
