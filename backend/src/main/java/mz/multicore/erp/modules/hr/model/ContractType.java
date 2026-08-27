package mz.multicore.erp.modules.hr.model;

/**
 * Tipos de contrato de trabalho. Ver docs/RH_COMPLETO_SPEC.md §B1.
 *
 * <p>A distinção que carrega regras é <b>a termo</b> vs <b>sem termo</b>: a lei laboral exige que um
 * contrato a termo diga <i>porquê</i> é a termo, e um contrato a termo sem data de fim não é um
 * contrato a termo — é um erro de preenchimento. Ambas as regras nascem daqui, e é por isso que a
 * pergunta se responde no tipo em vez de o serviço enumerar constantes.
 */
public enum ContractType {

    SEM_TERMO("Sem termo"),
    TERMO_CERTO("A termo certo"),
    TERMO_INCERTO("A termo incerto"),
    TEMPORARIO("Temporário"),
    ESTAGIO("Estágio");

    private final String label;

    ContractType(String label) {
        this.label = label;
    }

    /** Rótulo em PT-MZ. O operador nunca vê o nome da constante. */
    public String getLabel() {
        return label;
    }

    /**
     * Contratos com prazo. <b>Fonte única</b> da pergunta — o motivo é obrigatório nestes, e o fim
     * também. O {@code TERMO_INCERTO} conta: tem prazo, o que não tem é data certa à partida.
     */
    public boolean isFixedTerm() {
        return this != SEM_TERMO;
    }

    /**
     * Exige data de fim à partida. O termo incerto acaba quando a tarefa acaba, pelo que a data só
     * se conhece no fim — é o único a termo que pode ser gravado sem ela.
     */
    public boolean requiresEndDate() {
        return isFixedTerm() && this != TERMO_INCERTO;
    }
}
