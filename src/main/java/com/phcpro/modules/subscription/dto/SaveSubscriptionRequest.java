package com.phcpro.modules.subscription.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Define/actualiza o plano, preço e validade da assinatura de uma empresa. */
public record SaveSubscriptionRequest(
        @NotBlank String plan,
        BigDecimal monthlyPrice,
        LocalDate validUntil
) {}
