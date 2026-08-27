package mz.multicore.erp.modules.hr.model;

/**
 * Estado da folha de ponto de um mês. Ver docs/RH_COMPLETO_SPEC.md §B2.
 *
 * <p>O fecho é o que dá valor ao ponto: enquanto o mês está aberto, as horas apuradas são uma
 * fotografia que muda a cada marcação; fechado, passam a ser um facto em que a folha salarial pode
 * assentar. Reabrir é possível, mas fica auditado — senão o fecho não significa nada.
 */
public enum TimeSheetStatus {

    ABERTA("Aberta"),
    FECHADA("Fechada");

    private final String label;

    TimeSheetStatus(String label) {
        this.label = label;
    }

    /** Rótulo em PT-MZ. O operador nunca vê o nome da constante. */
    public String getLabel() {
        return label;
    }

    public boolean isClosed() {
        return this == FECHADA;
    }
}
