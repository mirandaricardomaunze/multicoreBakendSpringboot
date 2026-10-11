package mz.multicore.erp.modules.inventory.dto;

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
        LocalDateTime movementDate,
        Long businessUnitId,
        String businessUnitName
) {
    public StockMovementDTO(Long id, Long productId, String productName, Long warehouseId,
                            String warehouseName, BigDecimal quantity, String movementType,
                            String batchNumber, String serialNumber, String description,
                            LocalDateTime movementDate) {
        this(id, productId, productName, warehouseId, warehouseName, quantity, movementType,
                batchNumber, serialNumber, description, movementDate, null, null);
    }
}
