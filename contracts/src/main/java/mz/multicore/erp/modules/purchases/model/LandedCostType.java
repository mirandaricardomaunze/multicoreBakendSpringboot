package mz.multicore.erp.modules.purchases.model;

/**
 * Tipo de linha de um documento "Custos de Importação" (série CI, docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1).
 *
 * <p>Só os tipos {@link #capitalizable() capitalizáveis} entram no custo dos artigos (sem IVA, §4.2); o IVA
 * aduaneiro é imposto recuperável e vai todo a IVA dedutível (2432).
 */
public enum LandedCostType {

    FRETE("Frete", true),
    SEGURO("Seguro", true),
    DIREITOS_ADUANEIROS("Direitos aduaneiros", true),
    DESPACHANTE("Despachante", true),
    IVA_ADUANEIRO("IVA aduaneiro", false),
    OUTRO("Outro", true);

    private final String label;
    private final boolean capitalizable;

    LandedCostType(String label, boolean capitalizable) {
        this.label = label;
        this.capitalizable = capitalizable;
    }

    public String label() {
        return label;
    }

    /** {@code true} quando o valor sem IVA desta linha entra no custo dos artigos importados (§4.2). */
    public boolean capitalizable() {
        return capitalizable;
    }
}
