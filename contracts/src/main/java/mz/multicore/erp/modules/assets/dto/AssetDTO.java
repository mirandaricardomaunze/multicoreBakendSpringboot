package mz.multicore.erp.modules.assets.dto;

import mz.multicore.erp.modules.assets.model.AssetCategory;
import mz.multicore.erp.modules.assets.model.AssetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação imutável de um activo fixo patrimonial.
 */
public record AssetDTO(
        Long id,
        Long companyId,
        String assetCode,
        String name,
        AssetCategory category,
        AssetStatus status,
        LocalDate acquisitionDate,
        BigDecimal acquisitionCost,
        BigDecimal residualValue,
        BigDecimal annualDepreciationRate,
        BigDecimal accumulatedDepreciation,
        BigDecimal netBookValue,
        String serialNumber,
        String location,
        String assignedEmployee
) {}
