package mz.multicore.erp.modules.hr.model;

/**
 * Porque mudou o salário. Ver docs/RH_COMPLETO_SPEC.md §B4.
 *
 * <p>O motivo é obrigatório porque é a única coisa que distingue um aumento de uma correcção de
 * erro — e daqui a dois anos, quando alguém perguntar porque é que o ordenado subiu em Junho, é
 * isto que responde.
 */
public enum SalaryChangeReason {

    AUMENTO("Aumento"),
    PROMOCAO("Promoção"),
    REVISAO_ANUAL("Revisão anual"),
    ACORDO("Acordo"),
    CORRECCAO("Correcção"),
    /** Nasceu da activação de um contrato — o salário acordado passou a ser o vigente. */
    CONTRATO("Contrato");

    private final String label;

    SalaryChangeReason(String label) {
        this.label = label;
    }

    /** Rótulo em PT-MZ. O operador nunca vê o nome da constante. */
    public String getLabel() {
        return label;
    }
}
