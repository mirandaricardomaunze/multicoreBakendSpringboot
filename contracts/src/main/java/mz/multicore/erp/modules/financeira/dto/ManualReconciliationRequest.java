package mz.multicore.erp.modules.financeira.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Manual reconciliation request linking a statement line to a system transaction.
 */
public record ManualReconciliationRequest(
        @NotNull(message = "A linha do extracto é obrigatória.")
        Long statementItemId,

        @NotNull(message = "A transacção de tesouraria é obrigatória.")
        Long treasuryTransactionId
) {}
