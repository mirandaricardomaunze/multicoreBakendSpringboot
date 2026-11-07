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
        BigDecimal grossUnitWeightKg,
        Long businessUnitId,
        /** Perecível. {@code null} (cliente anterior) mantém o valor actual; na criação vale {@code false}. */
        Boolean tracksExpiry
) {
    /** Construtor compatível anterior à opção de produto perecível. */
    public CreateProductRequest(
            String sku, String reference, String barcode, String name,
            BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
            int unitsPerBox, Integer packagesPerBox, Integer unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId,
            String description, BigDecimal wholesalePrice, BigDecimal wholesaleMinQty,
            BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg, Long businessUnitId) {
        this(sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, packagesPerBox, unitsPerPackage, categoryId, saleType,
                stockTracked, taxRateId, description, wholesalePrice, wholesaleMinQty,
                netUnitWeightKg, grossUnitWeightKg, businessUnitId, null);
    }

    public CreateProductRequest(
            String sku, String reference, String barcode, String name,
            BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
            int unitsPerBox, Integer packagesPerBox, Integer unitsPerPackage,
            Long categoryId, String saleType, boolean stockTracked, Long taxRateId,
            String description, BigDecimal wholesalePrice, BigDecimal wholesaleMinQty,
            BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg) {
        this(sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                unitsPerBox, packagesPerBox, unitsPerPackage, categoryId, saleType,
                stockTracked, taxRateId, description, wholesalePrice, wholesaleMinQty,
                netUnitWeightKg, grossUnitWeightKg, null);
    }
}
