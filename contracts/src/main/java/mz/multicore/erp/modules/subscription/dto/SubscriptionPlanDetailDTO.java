package mz.multicore.erp.modules.subscription.dto;

import java.math.BigDecimal;

/**
 * Detalhe de um plano comercial da plataforma para apresentação e escolha do cliente.
 */
public record SubscriptionPlanDetailDTO(
        String plan,
        String label,
        BigDecimal monthlyPrice,
        String description
) {}
