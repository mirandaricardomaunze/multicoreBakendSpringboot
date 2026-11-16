package mz.multicore.erp.modules.pricing.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Creates or updates the price of a product (and tier) in a list. */
public record SavePriceListItemRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Tabela é obrigatória.") Long priceListId,
        @NotNull(message = "Produto é obrigatório.") Long productId,
        BigDecimal minQuantity,
        @NotNull(message = "O preço é obrigatório.") BigDecimal price
) {}
