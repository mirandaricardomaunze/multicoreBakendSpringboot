package mz.multicore.erp.modules.comercial.model;

/**
 * Nível executivo de risco de crédito de um cliente na gestão comercial.
 */
public enum CreditRiskLevel {
    LOW("Baixo", "Regular - Sem risco imediato"),
    MEDIUM("Médio", "Atenção - Atraso moderado ou dívida próxima ao limite"),
    HIGH("Alto", "Risco Elevado - Atraso > 30 dias ou limite excedido"),
    CRITICAL("Crítico", "Crítico - Atraso > 60 dias ou cliente com corte administrativo");

    private final String label;
    private final String description;

    CreditRiskLevel(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }
}
