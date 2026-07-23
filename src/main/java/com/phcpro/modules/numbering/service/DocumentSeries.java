package com.phcpro.modules.numbering.service;

/**
 * Séries de documentos do sistema. Cada série tem a sua numeração sequencial,
 * sem saltos, por ano. O prefixo é também o que aparece no número final
 * (ex.: série {@code "FT"} → {@code "FT-2026/1"}).
 */
public final class DocumentSeries {

    /** Fatura (inclui vendas POS, que são faturas-recibo reais). */
    public static final String INVOICE = "FT";
    /** Encomenda de cliente. */
    public static final String ORDER = "EC";
    /** Recibo de pagamento. */
    public static final String RECEIPT = "RC";
    /** Nota de crédito. */
    public static final String CREDIT_NOTE = "NC";
    /** Nota de débito. */
    public static final String DEBIT_NOTE = "ND";
    /** Transferência de stock entre armazéns. */
    public static final String STOCK_TRANSFER = "TRF";
    /** Guia de remessa ao cliente (mercadoria expedida a partir de uma encomenda). */
    public static final String DELIVERY_GUIDE = "GR";
    /** Documento interno de compra a fornecedor. */
    public static final String PURCHASE = "V/FT";
    /** Encomenda a fornecedor (purchase order). */
    public static final String PURCHASE_ORDER = "EC-F";
    /** Recibo de vencimento (folha salarial). */
    public static final String PAYSLIP = "REC";

    private DocumentSeries() {}
}
