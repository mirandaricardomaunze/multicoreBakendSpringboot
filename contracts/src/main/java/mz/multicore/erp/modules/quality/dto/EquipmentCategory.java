package mz.multicore.erp.modules.quality.dto;

public enum EquipmentCategory {
    PPE_PROTECTION("Protecção Individual (EPI)"),
    CALIBRATION_TOOL("Instrumento / Medição"),
    SAFETY_DEVICE("Dispositivo de Segurança"),
    WORK_TOOL("Ferramenta de Trabalho");

    private final String label;

    EquipmentCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
