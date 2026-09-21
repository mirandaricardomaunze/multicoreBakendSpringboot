package mz.multicore.erp.modules.financeira.dto;

/**
 * Pedido de reconciliação manual associando uma linha de extracto a uma transação do sistema.
 */
public record ManualReconciliationRequest(
        Long statementItemId,
        Long treasuryTransactionId
) {}
