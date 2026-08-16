package mz.multicore.erp.modules.comercial.model;

public enum InvoiceStatus {
    DRAFT,
    PENDING_APPROVAL,
    PENDING_DISCOUNT_APPROVAL,
    APPROVED,
    PARTIALLY_PAID,
    REJECTED,
    PAID,
    CANCELLED;

    /**
     * A fatura representa uma <b>venda realizada</b> — a mercadoria saiu e o stock baixou.
     * Inclui o fiado ({@code APPROVED}) e o pagamento parcial: só o recebimento é que ficou
     * por fazer. Exclui rascunhos, documentos à espera de aprovação (o stock ainda não se
     * moveu), rejeitados e anulados.
     *
     * <p>Fonte única de "isto conta como venda" — usada pelo dashboard e pelo relatório
     * diário, que antes tinham definições próprias e davam números diferentes.
     */
    public boolean isRealisedSale() {
        return this == APPROVED || this == PARTIALLY_PAID || this == PAID;
    }

    /**
     * A fatura pode ter saldo por cobrar. O valor em dívida vem de
     * {@code Invoice.outstandingAmount()} — este predicado só diz que vale a pena perguntar.
     */
    public boolean isCollectable() {
        return this == APPROVED || this == PARTIALLY_PAID;
    }

    /**
     * Os mesmos estados de {@link #isCollectable()}, em forma de colecção — para a pergunta ir
     * na consulta à base de dados em vez de se filtrar tudo em memória. Deriva do predicado,
     * para não haver duas listas a divergir.
     */
    public static java.util.List<InvoiceStatus> collectableStatuses() {
        return java.util.Arrays.stream(values()).filter(InvoiceStatus::isCollectable).toList();
    }

    /** Idem para {@link #isRealisedSale()}. */
    public static java.util.List<InvoiceStatus> realisedSaleStatuses() {
        return java.util.Arrays.stream(values()).filter(InvoiceStatus::isRealisedSale).toList();
    }
}
