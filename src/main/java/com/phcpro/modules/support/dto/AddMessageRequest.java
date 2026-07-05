package com.phcpro.modules.support.dto;

import jakarta.validation.constraints.NotBlank;

/** Nova mensagem numa conversa de assistência. */
public record AddMessageRequest(
        @NotBlank String body
) {}
