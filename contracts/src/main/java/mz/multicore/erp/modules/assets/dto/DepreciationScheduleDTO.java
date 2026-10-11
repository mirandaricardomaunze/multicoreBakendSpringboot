package mz.multicore.erp.modules.assets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parcela do mapa de depreciações de um activo fixo.
 */
public record DepreciationScheduleDTO(
        Long assetId,
        String assetCode,
        int periodYear,
        int periodMonth,
        LocalDate calculationDate,
        BigDecimal openingBookValue,
        BigDecimal depreciationAmount,
        BigDecimal closingBookValue,
        BigDecimal accumulatedDepreciation
) {}
