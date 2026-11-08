package mz.multicore.erp.modules.units.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Creates or updates the product unit {@code unitCode}. The server computes {@code factorToBase} when the
 * unit shares the base physical dimension (kg and g); otherwise it is required.
 */
public record SaveProductUnitRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Produto é obrigatório.") Long productId,
        @NotBlank(message = "Unidade é obrigatória.") String unitCode,
        BigDecimal factorToBase,
        String barcode,
        boolean sellable,
        boolean purchasable,
        boolean defaultSale,
        boolean defaultPurchase
) {}
