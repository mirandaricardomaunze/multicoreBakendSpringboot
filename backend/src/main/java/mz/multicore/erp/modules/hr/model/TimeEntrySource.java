package mz.multicore.erp.modules.hr.model;

/**
 * De onde veio a marcação. Ver docs/RH_COMPLETO_SPEC.md §B2.
 *
 * <p>A origem existe porque uma marcação escrita à mão por um chefe e uma vinda de um terminal não
 * merecem a mesma confiança — e quem audita a folha precisa de as distinguir sem ter de adivinhar.
 * A integração com relógio biométrico está fora desta iteração; {@code TERMINAL} fica declarado
 * para quando entrar.
 */
public enum TimeEntrySource {

    MANUAL("Manual"),
    IMPORTADO("Importado"),
    TERMINAL("Terminal");

    private final String label;

    TimeEntrySource(String label) {
        this.label = label;
    }

    /** Rótulo em PT-MZ. O operador nunca vê o nome da constante. */
    public String getLabel() {
        return label;
    }
}
