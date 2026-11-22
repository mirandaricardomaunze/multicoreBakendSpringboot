package mz.multicore.erp.modules.purchases.model;

/**
 * Natureza do documento guardado em {@code purchases}. As compras a fornecedor são {@link #PURCHASE};
 * {@link #OPENING_BALANCE} é o "Saldo Inicial Fornecedor" (série SIF) de uma factura por pagar trazida de outro ERP
 * (IMPORTACAO_DADOS_SPEC §5.3): sem linhas, sem stock e sem IVA, mas paga-se como uma compra.
 * {@link #LANDED_COST} é a dívida de uma linha de um documento "Custos de Importação" (CI) ao seu fornecedor
 * (MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC §4.1): sem linhas nem stock, com o IVA do serviço, e paga-se como uma compra.
 */
public enum PurchaseDocumentKind {
    PURCHASE,
    OPENING_BALANCE,
    LANDED_COST
}
