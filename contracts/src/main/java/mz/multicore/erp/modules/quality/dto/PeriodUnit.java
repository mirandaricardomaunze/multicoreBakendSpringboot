package mz.multicore.erp.modules.quality.dto;

public enum PeriodUnit {
    DAYS("Dias"),
    MONTHS("Meses");

    private final String label;

    PeriodUnit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
