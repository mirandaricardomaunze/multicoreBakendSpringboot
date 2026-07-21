package com.phcpro.modules.platform.dto;

import jakarta.validation.constraints.NotBlank;

/** Edição dos dados de uma empresa pelo superadmin (o NUIT é imutável). */
public record UpdateCompanyRequest(
        @NotBlank String name,
        String email,
        String address,
        String phone
) {}
