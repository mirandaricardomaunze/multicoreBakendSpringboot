package com.phcpro.modules.inventory.model;

/**
 * Tipo de movimento de stock. Quantidade positiva = entrada, negativa = saída.
 */
public enum StockMovementType {
    /** Entrada por compra a fornecedor. */
    PURCHASE,
    /** Entrada manual de stock. */
    ENTRY,
    /** Saída por venda. */
    SALE,
    /** Transferência entre armazéns (gera saída na origem e entrada no destino). */
    TRANSFER,
    /** Acerto manual de inventário (positivo ou negativo). */
    ADJUSTMENT,
    /** Entrada por devolução de cliente (nota de crédito). */
    RETURN,
    /** Estorno de uma venda anulada (reposição de stock). */
    REVERSAL
}
