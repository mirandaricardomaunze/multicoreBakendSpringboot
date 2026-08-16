package mz.multicore.erp.modules.purchases.model;

/**
 * Tipo de divergência na conferência à chegada.
 *
 * <p>São coisas diferentes e tratam-se de forma diferente: a mercadoria danificada <b>chegou</b>
 * (o fornecedor entregou e vai querer receber), a que falta <b>não chegou</b>. Metê-las no mesmo
 * saco era perder a única informação que serve para reclamar.
 */
public enum DiscrepancyType {

    /** Chegou, mas estragada ou fora de condições — não entra em stock. */
    DAMAGED("Danificada"),

    /** Não chegou e o operador declara que não virá (fecho curto da linha). */
    MISSING("Em falta");

    private final String label;

    DiscrepancyType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
