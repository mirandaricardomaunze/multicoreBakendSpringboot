package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;

/**
 * Sugestão analítica de reposição inteligente de stock.
 * Indica produtos em rutura ou abaixo do stock mínimo, velocidade de saída diária,
 * dias de stock restantes, fornecedor habitual e custo estimado de reabastecimento.
 */
public record ReorderSuggestionDTO(
        Long productId,
        String sku,
        String name,
        BigDecimal currentStock,
        BigDecimal minStock,
        int unitsPerBox,
        BigDecimal suggestedBoxes,
        BigDecimal suggestedUnits,
        BigDecimal dailySalesRate,
        Integer daysRemaining,
        String urgencyStatus,
        Long supplierId,
        String supplierName,
        BigDecimal estimatedUnitPrice,
        BigDecimal estimatedTotalCost
) {
    /** Construtor de compatibilidade para código ou testes que utilizam assinatura simplificada. */
    public ReorderSuggestionDTO(
            Long productId,
            String sku,
            String name,
            BigDecimal currentStock,
            BigDecimal minStock,
            int unitsPerBox,
            BigDecimal suggestedBoxes,
            BigDecimal suggestedUnits
    ) {
        this(
                productId,
                sku,
                name,
                currentStock,
                minStock,
                unitsPerBox,
                suggestedBoxes,
                suggestedUnits,
                BigDecimal.ZERO,
                currentStock != null && currentStock.signum() <= 0 ? 0 : null,
                currentStock != null && currentStock.signum() <= 0 ? "ESGOTADO" : "BAIXO",
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}
