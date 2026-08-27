package mz.multicore.erp.modules.purchases.dto;

import mz.multicore.erp.modules.purchases.model.DiscrepancyType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma ocorrência da conferência à chegada. */
public record GoodsReceiptDiscrepancyDTO(
        Long id,
        String orderNumber,
        String supplierName,
        String productName,
        DiscrepancyType type,
        String typeLabel,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        String notes,
        LocalDate occurredOn,
        boolean resolved,
        String resolutionNotes
) {}
