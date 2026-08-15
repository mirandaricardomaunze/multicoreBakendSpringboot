package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;

/**
 * @param paymentTermsDays prazo de pagamento acordado, em dias (0 = pronto pagamento)
 * @param creditLimit      tecto de dívida em aberto; {@code null} = sem limite (crédito livre)
 */
public record ClientDTO(
    Long id,
    String name,
    String taxId,
    String email,
    String address,
    int paymentTermsDays,
    BigDecimal creditLimit
) {}
