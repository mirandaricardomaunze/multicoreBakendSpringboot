package mz.multicore.erp.modules.pos.model;

/**
 * Estados do ciclo de vida de um Vale de Compras (Store Voucher).
 */
public enum StoreVoucherStatus {
    ACTIVE,          // Emitido e com saldo intacto
    PARTIALLY_USED,  // Utilizado parcialmente, com saldo remanescente
    FULLY_REDEEMED,  // Totalmente resgatado (saldo zero)
    EXPIRED,         // Ultrapassou a data limite de validade
    CANCELLED        // Anulado administrativamente
}
