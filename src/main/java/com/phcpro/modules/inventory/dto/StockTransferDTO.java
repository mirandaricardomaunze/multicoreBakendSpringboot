package com.phcpro.modules.inventory.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StockTransferDTO(
        Long id,
        String transferNumber,
        LocalDateTime transferDate,
        Long companyId,
        Long originWarehouseId,
        String originWarehouseName,
        Long destinationWarehouseId,
        String destinationWarehouseName,
        String status,
        String responsible,
        String vehicle,
        String notes,
        List<StockTransferLineDTO> lines
) {}
