package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Criação/edição de produto. Em edição (PUT /products/{id}) o {@code sku} é ignorado (identidade
 * vem do path). A composição nova preserva {@code unitsPerBox} para compatibilidade HTTP.
 */
public record CreateProductRequest(
        String sku,
        String reference,
        String barcode,
        @NotBlank String name,
        BigDecimal unitPrice,
        BigDecimal purchasePrice,
        BigDecimal minStock,
        int unitsPerBox,
        Integer packagesPerBox,
        Integer unitsPerPackage,
        Long categoryId,
        String saleType,
        boolean stockTracked,
        Long taxRateId,
        String description,
        BigDecimal wholesalePrice,
        BigDecimal wholesaleMinQty,
        BigDecimal netUnitWeightKg,
        BigDecimal grossUnitWeightKg
) {}
