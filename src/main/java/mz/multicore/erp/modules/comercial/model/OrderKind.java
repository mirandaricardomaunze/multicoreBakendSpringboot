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
    PICKING_REQUEST("Pedido de separação"),

    /**
     * Pedido de uma loja da própria empresa ao armazém que a abastece. Termina em
     * <b>transferência entre armazéns</b>, não em factura: a mercadoria muda de armazém e continua
     * a ser da empresa.
     *
     * <p>Não passa por aprovação porque a transferência já exige MANAGER/ADMIN para mover stock —
     * pedir autorização nas duas pontas seria pedir duas vezes a mesma coisa, para o mesmo acto.
     *
     * <p>Ver {@code docs/REPOSICAO_INTERNA_SPEC.md}.
     */
    INTERNAL_REPLENISHMENT("Reposição interna");

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
        return this != FORMAL_ORDER;
    }

    /** Percorre aguarda separação → em separação → separado. Trabalho de armazém. */
    public boolean usesSeparationFlow() {
        return this != FORMAL_ORDER;
    }

    /**
     * Se esta via termina em factura ao cliente.
     *
     * <p><b>É a trava que impede o stock de sair duas vezes.</b> Uma reposição interna termina em
     * transferência; se também pudesse ser facturada, a mercadoria saía uma vez na aprovação da
     * transferência e outra na facturação — e a segunda saída viria do armazém de onde ela já tinha
     * partido. A trava vive aqui, no documento, e não num aviso do ecrã.
     */
    public boolean isBillable() {
        return this != INTERNAL_REPLENISHMENT;
    }

    /** Só a reposição interna se converte em transferência entre armazéns. */
    public boolean usesWarehouseTransfer() {
        return this == INTERNAL_REPLENISHMENT;
    }

    /** A reposição interna precisa de saber para que loja vai. */
    public boolean requiresDestinationWarehouse() {
        return this == INTERNAL_REPLENISHMENT;
    }

    /**
     * A via a usar quando o pedido não a declara — {@link #FORMAL_ORDER}, que é o comportamento
     * que o sistema sempre teve na porta comercial (R1 da spec).
     */
    public static OrderKind orDefault(OrderKind kind) {
        return kind == null ? FORMAL_ORDER : kind;
    }
}
