package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;

/**
 * Pedido para registar um encargo ou comissão bancária diretamente a partir do extracto e reconciliar de imediato.
 */
public record CreateBankExpenseAndMatchRequest(
        Long statementItemId,
        String description,
        BigDecimal amount
) {}
