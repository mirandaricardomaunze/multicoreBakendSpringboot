package mz.multicore.erp.modules.hr.model;

/**
 * Porque é que o colaborador sai. Ver docs/RH_COMPLETO_SPEC.md §B3.
 *
 * <p>O motivo não é decoração: é ele que decide se o <b>aviso prévio não cumprido</b> se desconta.
 * Descontar aviso prévio a quem foi despedido seria cobrar-lhe a decisão da empresa.
 */
public enum TerminationReason {

    INICIATIVA_TRABALHADOR("Iniciativa do trabalhador", true),
    INICIATIVA_EMPREGADOR("Iniciativa do empregador", false),
    MUTUO_ACORDO("Mútuo acordo", false),
    FIM_DO_TERMO("Fim do termo", false),
    JUSTA_CAUSA("Justa causa", false);

    private final String label;
    private final boolean noticeOwedByEmployee;

    TerminationReason(String label, boolean noticeOwedByEmployee) {
        this.label = label;
        this.noticeOwedByEmployee = noticeOwedByEmployee;
    }

    public String getLabel() {
        return label;
    }

    /** O aviso prévio era devido <b>pelo trabalhador</b> — logo, não o cumprir desconta-lhe. */
    public boolean isNoticeOwedByEmployee() {
        return noticeOwedByEmployee;
    }
}
