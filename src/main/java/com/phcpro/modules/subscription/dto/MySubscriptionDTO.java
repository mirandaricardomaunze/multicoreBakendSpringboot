package com.phcpro.modules.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumo da assinatura para a própria empresa (assinante). Só-leitura. {@code daysRemaining} é
 * negativo se já expirou e {@code null} se não houver validade definida.
 */
public record MySubscriptionDTO(
        String companyName,
        boolean hasSubscription,
        String plan,
        String planLabel,
        String status,
        String statusLabel,
        LocalDate startDate,
        LocalDate validUntil,
        Long daysRemaining,
        BigDecimal monthlyPrice
) {}
