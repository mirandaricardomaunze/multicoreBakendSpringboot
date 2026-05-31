package com.phcpro.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateCreditNoteRequest(
        @NotNull Long invoiceId,
        @NotBlank String reason,        // CreditNoteReason name
        Long warehouseId,               // required if reason = RETURN
        String description,
        @NotEmpty @Valid List<CreateCreditNoteLineRequest> lines
) {}
