package mz.multicore.erp.modules.manufacturing.dto;

import mz.multicore.erp.modules.manufacturing.model.BomType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representação imutável de uma ficha técnica (BOM).
 */
public record BillOfMaterialsDTO(
        Long id,
        Long companyId,
        String bomCode,
        String name,
        Long finishedProductId,
        String finishedProductSku,
        String finishedProductName,
        BigDecimal baseQuantity,
        BomType bomType,
        boolean active,
        BigDecimal estimatedLaborCost,
        BigDecimal estimatedOverheadCost,
        BigDecimal totalEstimatedCost,
        List<BomItemDTO> items
) {}
