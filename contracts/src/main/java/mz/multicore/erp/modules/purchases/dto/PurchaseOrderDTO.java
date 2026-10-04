package mz.multicore.erp.modules.purchases.dto;

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
        List<PurchaseOrderLineDTO> lines,
        long version
) {
    /** Construtor retrocompatível sem versão optimista. */
    public PurchaseOrderDTO(
            Long id, String orderNumber, Long supplierId, String supplierName,
            Long warehouseId, Long companyId, LocalDate expectedDate, LocalDateTime orderDate,
            LocalDateTime receivedAt, BigDecimal totalAmount, BigDecimal taxAmount,
            String status, String notes, List<PurchaseOrderLineDTO> lines
    ) {
        this(id, orderNumber, supplierId, supplierName, warehouseId, companyId, expectedDate,
                orderDate, receivedAt, totalAmount, taxAmount, status, notes, lines, 0L);
    }
}
