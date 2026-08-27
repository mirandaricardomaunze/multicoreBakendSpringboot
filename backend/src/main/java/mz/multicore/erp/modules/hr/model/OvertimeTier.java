package mz.multicore.erp.modules.hr.model;

/**
 * Escalões de hora extra. Ver docs/RH_COMPLETO_SPEC.md §B2.
 *
 * <p>Existem separados porque a lei os paga de maneira diferente — somá-los num total só perderia
 * a informação que decide quanto se paga.
 */
public enum OvertimeTier {

    DIURNA("Extra diurna"),
    NOCTURNA("Extra nocturna"),
    DESCANSO("Dia de descanso ou feriado");

    private final String label;

    OvertimeTier(String label) {
        this.label = label;
    }

    /** Rótulo em PT-MZ. O operador nunca vê o nome da constante. */
    public String getLabel() {
        return label;
    }
}
