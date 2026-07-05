package com.phcpro.modules.platform.dto;

import java.util.List;

/** Visão global de um utilizador para o superadmin, com os seus acessos por empresa. */
public record PlatformUserDTO(
        Long id,
        String username,
        String name,
        String role,
        boolean active,
        boolean platformAdmin,
        List<CompanyRoleDTO> companies
) {
    public record CompanyRoleDTO(Long companyId, String companyName, String role) {}
}
