package mz.multicore.erp.modules.purchases.model;

/**
 * Natureza do documento guardado em {@code purchases}. As compras a fornecedor são {@link #PURCHASE};
 * {@link #OPENING_BALANCE} é o "Saldo Inicial Fornecedor" (série SIF) de uma factura por pagar trazida de outro ERP
 * (IMPORTACAO_DADOS_SPEC §5.3): sem linhas, sem stock e sem IVA, mas paga-se como uma compra.
 */
public enum PurchaseDocumentKind {
    PURCHASE,
    OPENING_BALANCE
}
