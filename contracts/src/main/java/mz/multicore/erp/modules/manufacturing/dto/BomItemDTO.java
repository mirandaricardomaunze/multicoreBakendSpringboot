package mz.multicore.erp.modules.manufacturing.dto;

import java.math.BigDecimal;

/**
 * Item / matéria-prima integrante de uma ficha técnica (BOM).
 */
public record BomItemDTO(
        Long id,
        Long rawMaterialProductId,
        String rawMaterialSku,
        String rawMaterialName,
        BigDecimal requiredQuantity,
        String unitOfMeasure,
        BigDecimal scrapPercentage,
        BigDecimal estimatedUnitCost
) {}
