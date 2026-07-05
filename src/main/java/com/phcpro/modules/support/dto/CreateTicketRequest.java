package com.phcpro.modules.support.dto;

import jakarta.validation.constraints.NotBlank;

/** Abertura de um pedido de assistência pela empresa. */
public record CreateTicketRequest(
        @NotBlank String subject,
        String description,
        String priority
) {}
