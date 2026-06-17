package com.phcpro.modules.comercial.model;

/**
 * Canal de origem de uma fatura. Permite distinguir vendas POS das faturas
 * manuais/de encomenda sem depender do prefixo do número (todas usam a série FT).
 */
public enum SalesChannel {
    MANUAL,
    POS,
    ORDER
}
