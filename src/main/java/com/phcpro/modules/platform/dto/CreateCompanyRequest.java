package com.phcpro.modules.platform.dto;

import jakarta.validation.constraints.NotBlank;

/** Onboarding de uma empresa nova pelo superadmin. */
public record CreateCompanyRequest(
        @NotBlank String name,
        @NotBlank String taxId,
        String email,
        String address
) {}
