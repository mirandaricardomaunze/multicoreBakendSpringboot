package com.phcpro.modules.pos.model;

/**
 * Métodos de pagamento aceites no POS.
 * (M-Pesa / mKesh propositadamente fora — não implementado nesta versão.)
 */
public enum PaymentMethod {
    CASH,            // Dinheiro
    CARD,            // Cartão (POS bancário)
    BANK_TRANSFER,   // Transferência
    CREDIT           // Fiado — cliente paga depois
}
