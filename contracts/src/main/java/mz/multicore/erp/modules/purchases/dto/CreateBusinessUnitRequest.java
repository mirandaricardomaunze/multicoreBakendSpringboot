package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBusinessUnitRequest(
        @NotBlank(message = "O código do negócio é obrigatório.")
        @Size(max = 30, message = "O código deve ter no máximo 30 caracteres.")
        String code,

        @NotBlank(message = "O nome do negócio é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name,

        Long supplierId,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String description
) {}
