package mz.multicore.erp.modules.pos.dto;

/**
 * Estados do ciclo de vida de uma transação de pagamento móvel Push USSD.
 */
public enum MobilePaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    EXPIRED,
    CANCELLED
}
