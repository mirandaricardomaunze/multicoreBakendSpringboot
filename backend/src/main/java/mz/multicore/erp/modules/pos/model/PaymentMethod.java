package mz.multicore.erp.modules.pos.model;

/**
 * Métodos de pagamento aceites no POS.
 * Os métodos eletrónicos (CARD, BANK_TRANSFER, MPESA, EMOLA) entram diretamente na tesouraria
 * (não na gaveta de numerário) e guardam a referência da transação.
 */
public enum PaymentMethod {
    CASH,            // Dinheiro
    CARD,            // Cartão (POS bancário)
    BANK_TRANSFER,   // Transferência
    MPESA,           // M-Pesa (Vodacom)
    EMOLA,           // e-Mola (Movitel)
    CREDIT           // Fiado — cliente paga depois
}
