package mz.multicore.erp.modules.inventory.model;

/** Ciclo de vida de uma sessão de contagem de inventário. */
public enum InventoryCountStatus {
    /** Em curso — contagens editáveis; ainda não mexeu no stock. */
    DRAFT,
    /** Aplicada — os ajustes foram lançados (stock acertado); só-leitura. */
    APPLIED,
    /** Cancelada — descartada sem tocar no stock. */
    CANCELLED
}
