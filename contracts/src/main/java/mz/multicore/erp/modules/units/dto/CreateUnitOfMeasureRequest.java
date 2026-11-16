package mz.multicore.erp.modules.units.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Company-owned unit. Packages only, because physical dimensions already exist in the system catalogue. */
public record CreateUnitOfMeasureRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotBlank(message = "O código da unidade é obrigatório.") String code,
        @NotBlank(message = "O nome da unidade é obrigatório.") String name,
        String symbol,
        @Min(value = 0, message = "As casas decimais não podem ser negativas.")
        @Max(value = 3, message = "No máximo 3 casas decimais.") int decimals
) {}
