package mz.multicore.erp.modules.comercial.model;

/**
 * Horizontes temporais do Radar Inteligente e Preventivo de Cobranças.
 * Permite segmentar dívidas vencidas e prevenir incumprimentos em faturas a vencer.
 */
public enum CollectionRadarBucket {

    CRITICAL_OVERDUE("Vencidas > 15 dias", true),
    RECENT_OVERDUE("Vencidas 1–15 dias", true),
    DUE_TODAY("Vencem Hoje", false),
    DUE_IN_7_DAYS("A Vencer em 7 dias", false),
    DUE_IN_15_DAYS("A Vencer em 15 dias", false),
    DUE_IN_30_DAYS("A Vencer em 30 dias", false),
    FUTURE("A Vencer > 30 dias", false);

    private final String label;
    private final boolean overdue;

    CollectionRadarBucket(String label, boolean overdue) {
        this.label = label;
        this.overdue = overdue;
    }

    public String label() {
        return label;
    }

    public boolean isOverdue() {
        return overdue;
    }

    /**
     * Classifica a fatura com base na diferença em dias entre a data de referência e a data de vencimento.
     * @param daysUntilDue dias até o vencimento (negativo = já vencido, zero = vence hoje, positivo = vence no futuro)
     */
    public static CollectionRadarBucket of(int daysUntilDue) {
        if (daysUntilDue < -15) return CRITICAL_OVERDUE;
        if (daysUntilDue < 0) return RECENT_OVERDUE;
        if (daysUntilDue == 0) return DUE_TODAY;
        if (daysUntilDue <= 7) return DUE_IN_7_DAYS;
        if (daysUntilDue <= 15) return DUE_IN_15_DAYS;
        if (daysUntilDue <= 30) return DUE_IN_30_DAYS;
        return FUTURE;
    }
}
