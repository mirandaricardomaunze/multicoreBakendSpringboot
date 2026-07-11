package com.phcpro.modules.inventory.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Sessão de contagem de inventário. {@code lines} vem vazio nas listagens (só no detalhe). */
public record InventoryCountDTO(
        Long id,
        Long warehouseId,
        String warehouseName,
        String status,
        String note,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime appliedAt,
        int totalItems,
        int countedItems,
        List<InventoryCountLineDTO> lines
) {}
