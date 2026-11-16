package mz.multicore.erp.modules.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Creates ({@code id=null}) or updates a price list. */
public record SavePriceListRequest(
        Long id,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotBlank(message = "O código da tabela é obrigatório.") String code,
        @NotBlank(message = "O nome da tabela é obrigatório.") String name,
        String description,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {}
