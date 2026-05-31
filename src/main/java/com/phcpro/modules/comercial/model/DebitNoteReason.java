package com.phcpro.modules.comercial.model;

/** Reason a debit note was issued. Debit notes never move stock. */
public enum DebitNoteReason {
    FREIGHT,        // Frete / portes adicionais
    SURCHARGE,      // Encargo extra (juros de mora, taxas)
    CORRECTION,     // Correção de valor em alta
    OTHER
}
