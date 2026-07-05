package com.phcpro.modules.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Concede (ou actualiza o papel de) acesso de um utilizador a uma empresa. */
public record GrantAccessRequest(
        @NotNull Long companyId,
        @NotBlank String role
) {}
