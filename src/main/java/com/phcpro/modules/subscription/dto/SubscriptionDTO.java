package com.phcpro.modules.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Visão da assinatura de uma empresa para o superadmin. {@code plan}/{@code status} são os nomes
 * dos enums (para pré-selecção na edição); os {@code *Label} são para apresentação. Empresa sem
 * assinatura tem {@code hasSubscription=false} e campos a null.
 */
public record SubscriptionDTO(
        Long companyId,
        String companyName,
        boolean companyActive,
        boolean hasSubscription,
        String plan,
        String planLabel,
        String status,
        String statusLabel,
        LocalDate startDate,
        LocalDate validUntil,
        BigDecimal monthlyPrice,
        long paymentCount
) {}
