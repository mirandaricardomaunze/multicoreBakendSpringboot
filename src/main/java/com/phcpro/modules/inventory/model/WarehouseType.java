package com.phcpro.modules.inventory.model;

/** Tipo/função de um armazém. */
public enum WarehouseType {
    STORE("Loja"),
    DEPOT("Depósito"),
    CENTRAL("Armazém Central"),
    TRANSIT("Trânsito");

    private final String label;

    WarehouseType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
