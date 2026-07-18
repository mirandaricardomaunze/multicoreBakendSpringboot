package com.phcpro.modules.platform.dto;

/** Visão da empresa para o superadmin: dados base + estado + nº de utilizadores. */
public record PlatformCompanyDTO(
        Long id,
        String name,
        String taxId,
        String email,
        String address,
        boolean active,
        long userCount
) {}
