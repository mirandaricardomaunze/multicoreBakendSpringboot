package mz.multicore.erp.modules.inventory.model;

/**
 * Motivo comercial e operacional da perda ou quebra de stock.
 */
public enum WasteReason {
    /** Validade do produto expirada. */
    EXPIRED("Validade Expirada"),
    /** Produto ou embalagem danificada / quebra física. */
    DAMAGED("Avaria / Quebra Física"),
    /** Falha na cadeia de frio / refrigeração. */
    COLD_CHAIN_FAILURE("Falha de Refrigeração"),
    /** Consumo interno, amostras ou degustação em loja. */
    INTERNAL_CONSUMPTION("Consumo Interno / Amostra"),
    /** Furto ou quebra desconhecida apurada em inventário. */
    THEFT_OR_SHRINKAGE("Furto / Quebra Desconhecida"),
    /** Outro motivo justificado operacionalmente. */
    OTHER("Outro Motivo");

    private final String description;

    WasteReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
