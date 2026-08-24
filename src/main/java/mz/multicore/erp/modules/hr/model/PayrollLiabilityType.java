package mz.multicore.erp.modules.hr.model;

/**
 * O que é que a empresa reteve e ainda não entregou. Ver docs/RH_COMPLETO_SPEC.md §B5.
 *
 * <p>O INSS patronal está aqui apesar de <b>não ser retenção</b>: é custo da empresa, não desconto
 * ao trabalhador. Mas é dinheiro que sai pela mesma porta, no mesmo prazo e para a mesma entidade,
 * e tê-lo fora desta lista era exactamente como ele desaparecia até agora.
 */
public enum PayrollLiabilityType {

    IRPS("IRPS retido", true),
    INSS_TRABALHADOR("INSS — quota do trabalhador", true),
    INSS_PATRONAL("INSS — encargo da empresa", false);

    private final String label;
    private final boolean withheldFromEmployee;

    PayrollLiabilityType(String label, boolean withheldFromEmployee) {
        this.label = label;
        this.withheldFromEmployee = withheldFromEmployee;
    }

    public String getLabel() {
        return label;
    }

    /** Foi descontado ao trabalhador (contra: é encargo da empresa). */
    public boolean isWithheldFromEmployee() {
        return withheldFromEmployee;
    }

    /** As duas quotas do INSS entregam-se juntas, e é por isso que partilham o prazo. */
    public boolean isSocialSecurity() {
        return this != IRPS;
    }
}
