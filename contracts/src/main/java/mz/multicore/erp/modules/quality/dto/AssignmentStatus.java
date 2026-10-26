package mz.multicore.erp.modules.quality.dto;

public enum AssignmentStatus {
    ACTIVE("Activo"),
    EXPIRING_SOON("A Expirar"),
    EXPIRED("Expirado"),
    RENEWED("Renovado"),
    RETURNED("Devolvido"),
    DISCARDED("Descartado");

    private final String label;

    AssignmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
