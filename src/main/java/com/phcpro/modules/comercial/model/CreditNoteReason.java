package com.phcpro.modules.comercial.model;

/**
 * Why a credit note was issued.
 * Only {@link #RETURN} returns physical stock to the warehouse when approved —
 * the other reasons are purely financial adjustments.
 */
public enum CreditNoteReason {
    RETURN,         // Cliente devolve produtos — restitui stock no armazém
    DISCOUNT,       // Desconto comercial pós-emissão da fatura
    ERROR,          // Correção de valor / dado fiscal incorreto
    CANCELLATION    // Anulação total da fatura
}
