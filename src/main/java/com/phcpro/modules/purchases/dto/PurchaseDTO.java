package com.phcpro.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseDTO(
        Long id,
        String purchaseNumber,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        Long companyId,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        String status,
        LocalDateTime purchaseDate,
        List<PurchaseLineDTO> lines
) {}
