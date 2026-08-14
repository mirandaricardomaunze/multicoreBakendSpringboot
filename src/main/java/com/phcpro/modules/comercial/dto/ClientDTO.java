package com.phcpro.modules.comercial.dto;

/**
 * @param paymentTermsDays prazo de pagamento acordado, em dias (0 = pronto pagamento)
 */
public record ClientDTO(
    Long id,
    String name,
    String taxId,
    String email,
    String address,
    int paymentTermsDays
) {}
