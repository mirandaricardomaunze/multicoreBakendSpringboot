package mz.multicore.erp.modules.platform.dto;

import jakarta.validation.constraints.NotBlank;

/** Edição dos dados de um utilizador pelo superadmin (o username é imutável — é a identidade). */
public record UpdatePlatformUserRequest(
        @NotBlank String name
) {}
