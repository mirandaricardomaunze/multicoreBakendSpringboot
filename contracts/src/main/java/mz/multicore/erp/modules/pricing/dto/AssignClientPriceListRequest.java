package mz.multicore.erp.modules.pricing.dto;

import jakarta.validation.constraints.NotNull;

/** Assigns ({@code priceListId} set) or removes ({@code null}) the price list of a client. */
public record AssignClientPriceListRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Cliente é obrigatório.") Long clientId,
        Long priceListId
) {}
