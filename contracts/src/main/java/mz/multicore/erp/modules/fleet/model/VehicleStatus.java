package mz.multicore.erp.modules.fleet.model;

/**
 * Estados operacionais de um veículo da frota.
 */
public enum VehicleStatus {
    OPERATIONAL("Operacional / Em Serviço"),
    MAINTENANCE("Em Manutenção"),
    INSPECTION("Em Inspecção"),
    OUT_OF_SERVICE("Fora de Serviço / Avariado"),
    SOLD("Alienado / Vendido");

    private final String label;

    VehicleStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
