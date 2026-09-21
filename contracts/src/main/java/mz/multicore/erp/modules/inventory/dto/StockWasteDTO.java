package mz.multicore.erp.modules.inventory.dto;

import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.inventory.model.WasteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockWasteDTO(
        Long id,
        Long companyId,
        Long warehouseId,
        String warehouseName,
        Long productId,
        String productSku,
        String productName,
        Long batchId,
        String batchNumber,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal totalCost,
        WasteReason reason,
        WasteStatus status,
        String notes,
        String registeredBy,
        String approvedBy,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        Long stockMovementId
) {}
