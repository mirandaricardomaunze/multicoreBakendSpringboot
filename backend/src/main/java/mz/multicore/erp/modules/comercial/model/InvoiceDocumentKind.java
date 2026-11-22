package mz.multicore.erp.modules.comercial.model;

/**
 * Natureza do documento guardado em {@code invoices}. Todas as vendas são {@link #SALE}; {@link #OPENING_BALANCE} é o
 * "Saldo Inicial" (série SI) de uma factura em aberto trazida de outro ERP (IMPORTACAO_DADOS_SPEC §5.2): não é
 * documento fiscal nem venda, mas cobra-se e liquida-se como uma factura.
 */
public enum InvoiceDocumentKind {
    SALE,
    OPENING_BALANCE
}
