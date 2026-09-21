package mz.multicore.erp.modules.inventory.dto;

import mz.multicore.erp.modules.inventory.model.WasteReason;

import java.math.BigDecimal;

public record CreateStockWasteRequest(
        Long companyId,
        Long warehouseId,
        Long productId,
        Long batchId,
        BigDecimal quantity,
        WasteReason reason,
        String notes
) {}
