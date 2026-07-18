package com.phcpro.modules.users.dto;

/** Vista de leitura de um utilizador para a UI (sem password). */
public record AppUserDTO(
        Long id,
        String username,
        String name,
        String role,
        boolean active
) {}
