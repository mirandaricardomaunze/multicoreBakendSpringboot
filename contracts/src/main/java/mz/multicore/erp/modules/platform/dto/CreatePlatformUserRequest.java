package mz.multicore.erp.modules.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Criação de um utilizador pelo superadmin, já ligado a uma empresa com um papel. */
public record CreatePlatformUserRequest(
        @NotBlank String username,
        @NotBlank String name,
        @NotBlank String password,
        @NotNull Long companyId,
        @NotBlank String companyRole
) {}
