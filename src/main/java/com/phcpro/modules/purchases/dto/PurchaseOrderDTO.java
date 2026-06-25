package com.phcpro.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderDTO(
        Long id,
        String orderNumber,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        Long companyId,
        LocalDate expectedDate,
        LocalDateTime orderDate,
        LocalDateTime receivedAt,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        String status,
        String notes,
        List<PurchaseOrderLineDTO> lines
) {}
