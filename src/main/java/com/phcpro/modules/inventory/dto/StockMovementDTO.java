package com.phcpro.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementDTO(
        Long id,
        Long productId,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal quantity,
        String movementType,
        String batchNumber,
        String serialNumber,
        String description,
        LocalDateTime movementDate
) {}
