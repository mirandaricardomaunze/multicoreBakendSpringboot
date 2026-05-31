package com.phcpro.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateDebitNoteRequest(
        @NotNull Long invoiceId,
        @NotBlank String reason,   // DebitNoteReason name
        String description,
        @NotEmpty @Valid List<CreateDebitNoteLineRequest> lines
) {}
