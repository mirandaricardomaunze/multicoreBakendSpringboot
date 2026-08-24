package mz.multicore.erp.modules.hr.model;

import java.util.Set;

/**
 * <b>Que tipos de falta descontam no recibo — declarado, não acidental.</b>
 * Ver docs/RH_COMPLETO_SPEC.md §B8.5.
 *
 * <p>Até aqui a regra vivia numa consulta: {@code findUnjustifiedOverlapping} filtrava
 * {@code absenceType = 'UNJUSTIFIED'} e mais nada. O resultado — baixa médica e maternidade pagas —
 * estava provavelmente <b>certo</b>, mas estava certo <b>por acidente</b>: nenhum tipo de falta
 * tinha regra de remuneração declarada em lado nenhum, e o dia em que alguém acrescentasse
 * {@code LICENCA_SEM_VENCIMENTO} essa falta passava a ser paga sem ninguém ter decidido isso.
 *
 * <p>Uma lista explícita torna a decisão visível e revisível. O que não estiver aqui <b>não
 * desconta</b>, que é o desfecho seguro para o trabalhador: descontar por omissão tira dinheiro a
 * quem talvez não devesse perdê-lo, e ninguém repara até haver reclamação.
 */
public final class AbsencePayRule {

    /** Falta injustificada: dia não trabalhado e não remunerado. */
    public static final String UNJUSTIFIED = "UNJUSTIFIED";
    /** Licença sem vencimento: acordada, e por isso mesmo não paga. */
    public static final String UNPAID_LEAVE = "UNPAID_LEAVE";
    /** Nascida do fecho do ponto e ainda por decidir — <b>não desconta</b> até alguém decidir. */
    public static final String PENDING_JUSTIFICATION = "PENDING_JUSTIFICATION";

    /** Os tipos que descontam. Tudo o resto é remunerado, por decisão e não por omissão. */
    private static final Set<String> UNPAID = Set.of(UNJUSTIFIED, UNPAID_LEAVE);

    private AbsencePayRule() {}

    /** Esta falta desconta no recibo. */
    public static boolean isUnpaid(String absenceType) {
        return absenceType != null && UNPAID.contains(absenceType.trim().toUpperCase());
    }

    /** Os tipos que descontam, para quem precisa de os passar a uma consulta. */
    public static Set<String> unpaidTypes() {
        return UNPAID;
    }
}
