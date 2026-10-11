package mz.multicore.erp.modules.assets.model;

/**
 * Estados do ciclo de vida de um activo fixo.
 */
public enum AssetStatus {
    ACTIVE("Em Operação"),
    DEPRECIATING("Em Depreciação"),
    FULLY_DEPRECIATED("Totalmente Depreciado"),
    MAINTENANCE("Em Manutenção"),
    WRITTEN_OFF("Abatido"),
    SOLD("Alienado / Vendido");

    private final String label;

    AssetStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
