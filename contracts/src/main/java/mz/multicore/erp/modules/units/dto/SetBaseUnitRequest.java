package mz.multicore.erp.modules.units.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Sets the base unit of a product that has no movements or alternative units yet. */
public record SetBaseUnitRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @NotBlank(message = "Unidade é obrigatória.") String unitCode
) {}
