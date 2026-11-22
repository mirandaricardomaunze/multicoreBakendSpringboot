package mz.multicore.erp.modules.purchases.model;

/**
 * Método de repartição do custo de um documento "Custos de Importação" pelas linhas das compras
 * (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.3). {@link #VALOR} é o método por omissão.
 */
public enum LandedCostAllocationMethod {

    /** Valor em moeda base de cada linha de compra. */
    VALOR("Valor"),
    /** {@code netUnitWeightKg × quantidade}; todas as linhas têm de ter peso. */
    PESO("Peso"),
    /** Quantidade recebida de cada linha. */
    QUANTIDADE("Quantidade");

    private final String label;

    LandedCostAllocationMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** O método escolhido ou, quando não indicado, o de omissão ({@link #VALOR}). */
    public static LandedCostAllocationMethod orDefault(LandedCostAllocationMethod method) {
        return method == null ? VALOR : method;
    }
}
