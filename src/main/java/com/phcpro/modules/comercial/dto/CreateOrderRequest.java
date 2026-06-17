package com.phcpro.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pedido para criar uma encomenda. O cliente é opcional — se {@code clientId} for nulo,
 * a encomenda fica para "Consumidor Final" e {@code walkInName} (se preenchido) é usado
 * como rótulo livre no descritor, sem criar registo de cliente.
 *
 * <p>Distinto de {@code CreateInvoiceRequest} para manter o cliente <strong>obrigatório</strong>
 * em facturas (requisito fiscal) sem afectar este fluxo.
 */
public record CreateOrderRequest(
        Long clientId,
        @Size(max = 120, message = "Nome do comprador deve ter no máximo 120 caracteres.")
        String walkInName,
        @NotNull(message = "O ID da empresa é obrigatório.") Long companyId,
        @NotNull(message = "O ID do armazém é obrigatório.") Long warehouseId,
        @NotEmpty(message = "A encomenda deve conter pelo menos uma linha.") @Valid
        List<CreateInvoiceLineRequest> lines
) {}
