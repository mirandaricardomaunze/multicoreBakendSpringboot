package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

/** Período do dia em que a entrega está agendada (docs/ENTREGAS_AGENDA_PROVA_SPEC.md §3.1). */
public enum DeliveryPeriod {

    MORNING("Manhã"),
    AFTERNOON("Tarde");

    private final String label;

    DeliveryPeriod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Lê o período enviado pelo cliente; valores desconhecidos são uma regra de negócio violada. */
    public static DeliveryPeriod parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Indique o período da entrega (Manhã ou Tarde).");
        }
        for (DeliveryPeriod period : values()) {
            if (period.name().equalsIgnoreCase(value.trim())) {
                return period;
            }
        }
        throw new BusinessRuleException("Período de entrega inválido: use Manhã ou Tarde.");
    }
}
