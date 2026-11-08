package mz.multicore.erp.modules.activity.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Replaces the company profiles; at least one profile is required. */
public record UpdateCompanyActivityRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotEmpty(message = "Escolha pelo menos um perfil de actividade.") List<String> profiles
) {}
