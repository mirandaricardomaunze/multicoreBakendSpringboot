package mz.multicore.erp.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representação completa da sessão de inventário físico com totais consolidados.
 */
public record InventorySessionDTO(
        Long id,
        String inventoryNumber,
        String description,
        InventoryStatus status,
        boolean blindCounting,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long companyId,
        int totalItems,
        int itemsCounted,
        BigDecimal totalSurplusValue,
        BigDecimal totalDeficitValue,
        BigDecimal netFinancialImpact,
        List<InventoryItemDTO> items,
        String notes
) {
    public InventorySessionDTO {
        if (totalSurplusValue == null) totalSurplusValue = BigDecimal.ZERO;
        if (totalDeficitValue == null) totalDeficitValue = BigDecimal.ZERO;
        if (netFinancialImpact == null) netFinancialImpact = totalSurplusValue.subtract(totalDeficitValue);
        if (items == null) items = List.of();
    }
}
