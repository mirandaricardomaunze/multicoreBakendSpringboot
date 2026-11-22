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
        long version,
        Long businessUnitId,
        String businessUnitName,
        /** Moeda do documento; preços e totais estão nesta moeda. */
        String currency,
        /** Câmbio gravado no documento (unidades da moeda base por 1 unidade da moeda). */
        BigDecimal exchangeRate,
        /** Total em moeda base = total × câmbio, 2 casas (gravado no documento). */
        BigDecimal baseTotalAmount
) {
    /** Construtor retrocompatível, anterior à moeda estrangeira: moeda base com câmbio 1. */
    public PurchaseOrderDTO(
            Long id, String orderNumber, Long supplierId, String supplierName,
            Long warehouseId, Long companyId, LocalDate expectedDate, LocalDateTime orderDate,
            LocalDateTime receivedAt, BigDecimal totalAmount, BigDecimal taxAmount,
            String status, String notes, List<PurchaseOrderLineDTO> lines, long version,
            Long businessUnitId, String businessUnitName
    ) {
        this(id, orderNumber, supplierId, supplierName, warehouseId, companyId, expectedDate, orderDate, receivedAt,
                totalAmount, taxAmount, status, notes, lines, version, businessUnitId, businessUnitName,
                null, BigDecimal.ONE, totalAmount);
    }

    /** Construtor retrocompatível com 15 argumentos (sem unidade de negócio). */
    public PurchaseOrderDTO(
            Long id, String orderNumber, Long supplierId, String supplierName,
            Long warehouseId, Long companyId, LocalDate expectedDate, LocalDateTime orderDate,
            LocalDateTime receivedAt, BigDecimal totalAmount, BigDecimal taxAmount,
            String status, String notes, List<PurchaseOrderLineDTO> lines, long version
    ) {
        this(id, orderNumber, supplierId, supplierName, warehouseId, companyId, expectedDate,
                orderDate, receivedAt, totalAmount, taxAmount, status, notes, lines, version, null, null);
    }

    /** Construtor retrocompatível sem versão optimista. */
    public PurchaseOrderDTO(
            Long id, String orderNumber, Long supplierId, String supplierName,
            Long warehouseId, Long companyId, LocalDate expectedDate, LocalDateTime orderDate,
            LocalDateTime receivedAt, BigDecimal totalAmount, BigDecimal taxAmount,
            String status, String notes, List<PurchaseOrderLineDTO> lines
    ) {
        this(id, orderNumber, supplierId, supplierName, warehouseId, companyId, expectedDate,
                orderDate, receivedAt, totalAmount, taxAmount, status, notes, lines, 0L, null, null);
    }
}
