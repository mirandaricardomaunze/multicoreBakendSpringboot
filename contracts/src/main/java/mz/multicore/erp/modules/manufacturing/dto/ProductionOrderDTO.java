package mz.multicore.erp.modules.manufacturing.dto;

import mz.multicore.erp.modules.manufacturing.model.ProductionOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação imutável de uma Ordem de Produção (OP).
 */
public record ProductionOrderDTO(
        Long id,
        Long companyId,
        String orderNumber,
        Long bomId,
        String bomCode,
        Long finishedProductId,
        String finishedProductSku,
        String finishedProductName,
        BigDecimal plannedQuantity,
        BigDecimal producedQuantity,
        ProductionOrderStatus status,
        LocalDate startDate,
        LocalDate plannedEndDate,
        LocalDate completionDate,
        Long sourceWarehouseId,
        Long targetWarehouseId,
        BigDecimal totalProductionCost,
        BigDecimal unitProductionCost
) {}
