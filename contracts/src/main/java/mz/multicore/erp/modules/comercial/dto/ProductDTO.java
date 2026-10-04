package mz.multicore.erp.modules.comercial.dto;

import mz.multicore.erp.architecture.pricing.TaxRates;

import java.math.BigDecimal;

public record ProductDTO(
    Long id,
    String sku,
    String reference,
    String barcode,
    String name,
    BigDecimal unitPrice,
    BigDecimal purchasePrice,
    BigDecimal minStock,
    BigDecimal wholesalePrice,
    BigDecimal wholesaleMinQty,
    int unitsPerBox,
    int packagesPerBox,
    int unitsPerPackage,
    String saleType,
    boolean stockTracked,
    Long categoryId,
    String categoryName,
    Long taxRateId,
    BigDecimal taxRate,
    String taxRateLabel,
    String description,
    byte[] image,
    BigDecimal netUnitWeightKg,
    BigDecimal grossUnitWeightKg,
    Long version
) {
    public ProductDTO {
        unitsPerBox = Math.max(1, unitsPerBox);
        if (packagesPerBox <= 0 || unitsPerPackage <= 0) {
            packagesPerBox = unitsPerBox;
            unitsPerPackage = 1;
        }
    }

    public ProductDTO(
            Long id, String sku, String reference, String barcode, String name,
            BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty, int unitsPerBox,
            int packagesPerBox, int unitsPerPackage, String saleType, boolean stockTracked,
            Long categoryId, String categoryName, Long taxRateId, BigDecimal taxRate,
            String taxRateLabel, String description, byte[] image, BigDecimal netUnitWeightKg,
            BigDecimal grossUnitWeightKg) {
        this(id, sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                wholesalePrice, wholesaleMinQty, unitsPerBox, packagesPerBox, unitsPerPackage,
                saleType, stockTracked, categoryId, categoryName, taxRateId, taxRate,
                taxRateLabel, description, image, netUnitWeightKg, grossUnitWeightKg, 0L);
    }

    /** Construtor compatível para código cliente que ainda conhece apenas unidades por caixa. */
    public ProductDTO(
            Long id, String sku, String reference, String barcode, String name,
            BigDecimal unitPrice, BigDecimal purchasePrice, BigDecimal minStock,
            BigDecimal wholesalePrice, BigDecimal wholesaleMinQty, int unitsPerBox,
            String saleType, boolean stockTracked, Long categoryId, String categoryName,
            Long taxRateId, BigDecimal taxRate, String taxRateLabel, String description,
            byte[] image, BigDecimal netUnitWeightKg, BigDecimal grossUnitWeightKg) {
        this(id, sku, reference, barcode, name, unitPrice, purchasePrice, minStock,
                wholesalePrice, wholesaleMinQty, unitsPerBox, Math.max(1, unitsPerBox), 1,
                saleType, stockTracked, categoryId, categoryName, taxRateId, taxRate,
                taxRateLabel, description, image, netUnitWeightKg, grossUnitWeightKg);
    }

    public BigDecimal grossBoxWeightKg() {
        return grossUnitWeightKg == null ? BigDecimal.ZERO
                : grossUnitWeightKg.multiply(BigDecimal.valueOf(Math.max(1, unitsPerBox)));
    }
    /**
     * Espelho no lado do cliente da regra de {@code Product.effectiveTaxRate()}: taxa do cadastro e,
     * na ausência dela, a taxa-padrão. Existe para que a pré-visualização de totais nos painéis
     * mostre exactamente o que o backend vai cobrar — nunca para decidir imposto, que é sempre
     * resolvido no servidor. Ver docs/IVA_TAXA_CANONICA_SPEC.md.
     */
    public BigDecimal effectiveTaxRate() {
        return taxRate != null ? taxRate : TaxRates.STANDARD_VAT;
    }
}
