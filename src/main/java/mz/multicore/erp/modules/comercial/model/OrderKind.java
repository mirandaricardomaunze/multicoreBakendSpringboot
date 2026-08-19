package mz.multicore.erp.modules.comercial.model;

/**
 * De que <b>via</b> é esta encomenda — declarado na criação e gravado no documento.
 *
 * <p>O sistema tem dois circuitos de encomenda desde alturas diferentes. Antes desta enumeração,
 * qual deles se aplicava era <b>adivinhado a partir do estado</b>: quem imprimia perguntava "está
 * em separação?" e, se não estivesse, assumia A4. Um estado novo no circuito de separação fazia
 * sair o documento errado sem ninguém dar por isso.
 *
 * <p>A via é um facto do documento, não uma consequência do sítio onde ele está parado. É ela —
 * e só ela — que decide três coisas: se precisa de aprovação, que documento se imprime, e se
 * entra no circuito de separação do armazém.
 *
 * <p>Ver {@code docs/ENCOMENDA_DUAS_VIAS_SPEC.md}.
 */
public enum OrderKind {

    /**
     * Compromisso comercial formal com o cliente. Passa pelo motor de aprovações antes de ser
     * faturável e imprime em A4 com o mesmo desenho da fatura.
     */
    FORMAL_ORDER("Encomenda (A4)"),

    /**
     * Trabalho interno de armazém: reserva stock, imprime talão térmico e segue para separação.
     * Não passa por aprovação — o dinheiro e o stock só se movem na facturação, que tem as suas
     * próprias travas (limite de crédito, stock disponível).
     */
    PICKING_REQUEST("Pedido de separação");

    private final String label;

    OrderKind(String label) {
        this.label = label;
    }

    /** Rótulo em PT-MZ. O operador nunca vê o nome da constante. */
    public String label() {
        return label;
    }

    public boolean requiresApproval() {
        return this == FORMAL_ORDER;
    }

    /** Talão de 80 mm (com o desenho do recibo do POS) em vez de A4. */
    public boolean isThermal() {
        return this == PICKING_REQUEST;
    }

    /** Só o pedido de separação percorre aguarda separação → em separação → separado. */
    public boolean usesSeparationFlow() {
        return this == PICKING_REQUEST;
    }

    /**
     * A via a usar quando o pedido não a declara — {@link #FORMAL_ORDER}, que é o comportamento
     * que o sistema sempre teve na porta comercial (R1 da spec).
     */
    public static OrderKind orDefault(OrderKind kind) {
        return kind == null ? FORMAL_ORDER : kind;
    }
}
