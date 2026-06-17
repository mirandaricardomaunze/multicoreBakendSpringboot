package com.phcpro.modules.financeira.model;

/**
 * Tipo de movimento de tesouraria.
 * Convenção interna do sistema (ver {@code FinanceService}):
 * DEBIT = entrada/receita (aumenta o saldo); CREDIT = saída/despesa (diminui o saldo).
 */
public enum TransactionType {
    DEBIT,
    CREDIT
}
