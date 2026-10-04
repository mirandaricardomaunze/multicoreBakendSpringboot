package mz.multicore.erp.modules.financeira.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Pedido de reconciliação manual associando uma linha de extracto a uma transação do sistema.
 */
public record ManualReconciliationRequest(
        @NotNull(message = "A linha do extracto é obrigatória.")
        Long statementItemId,

        @NotNull(message = "A transacção de tesouraria é obrigatória.")
        Long treasuryTransactionId
) {}
