package com.phcpro.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateInvoiceRequest(
    @NotNull(message = "O ID do cliente é obrigatório.")
    Long clientId,

    @NotNull(message = "O ID da empresa é obrigatório.")
    Long companyId,

    @NotNull(message = "O ID do armazém é obrigatório.")
    Long warehouseId,

    @NotEmpty(message = "A fatura deve conter pelo menos uma linha.")
    @Valid
    List<CreateInvoiceLineRequest> lines
) {}
