package com.phcpro.modules.comercial.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Criação/edição de produto. Em edição (PUT /products/{id}) o {@code sku} é ignorado (identidade
 * vem do path). Espelha os 15 campos do {@code ComercialService.createProduct/updateProduct}.
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
        Long categoryId,
        String saleType,
        boolean stockTracked,
        Long taxRateId,
        String description,
        BigDecimal wholesalePrice,
        BigDecimal wholesaleMinQty
) {}
