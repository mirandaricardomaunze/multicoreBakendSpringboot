package com.phcpro.modules.comercial.dto;

import com.phcpro.architecture.pricing.TaxRates;

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
    String saleType,
    boolean stockTracked,
    Long categoryId,
    String categoryName,
    Long taxRateId,
    BigDecimal taxRate,
    String taxRateLabel,
    String description,
    byte[] image
) {
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
