package com.phcpro.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Pedido de criação de uma Guia de Remessa a partir de uma encomenda aprovada.
 * A empresa, o cliente, o armazém e as linhas derivam da encomenda — aqui só entra a origem
 * (encomenda) e os dados livres de transporte.
 */
public record CreateDeliveryGuideRequest(
        @NotNull Long orderId,
        String responsible,
        String vehicle,
        String notes
) {}
