package com.phcpro.modules.financeira.dto;

import jakarta.validation.constraints.NotNull;

public record PayInvoiceRequest(
    @NotNull(message = "O ID da fatura é obrigatório.")
    Long invoiceId,

    @NotNull(message = "O ID da conta de tesouraria é obrigatório.")
    Long accountId
) {}
