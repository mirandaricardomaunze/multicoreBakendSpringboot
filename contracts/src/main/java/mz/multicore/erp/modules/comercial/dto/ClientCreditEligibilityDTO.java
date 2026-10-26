package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;

/**
 * Análise de elegibilidade de crédito de um cliente para encomenda e faturação a prazo.
 *
 * <p>Usado no desktop e no backend para determinar se o cliente pode comprar a prazo ou se
 * deve ser estritamente limitado a pronto pagamento devido a restrições de crédito (mora de
 * faturas vencidas, limite esgotado ou ausência de cadastro de cliente).
 */
public record ClientCreditEligibilityDTO(
        Long clientId,
        String clientName,
        boolean creditAllowed,
        boolean problematic,
        String restrictionReason,
        int defaultPaymentTermsDays,
        BigDecimal creditLimit,
        BigDecimal totalDebt,
        BigDecimal availableCredit,
        int maxDaysOverdue
) {}
