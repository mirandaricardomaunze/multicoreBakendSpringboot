package mz.multicore.erp.modules.purchases.dto;

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
        BigDecimal amountPaid,
        String status,
        LocalDateTime purchaseDate,
        List<PurchaseLineDTO> lines,
        Long businessUnitId,
        String businessUnitName
) {
    /** Construtor retrocompatível para 12 argumentos. */
    public PurchaseDTO(
            Long id, String purchaseNumber, Long supplierId, String supplierName,
            Long warehouseId, Long companyId, BigDecimal totalAmount, BigDecimal taxAmount,
            BigDecimal amountPaid, String status, LocalDateTime purchaseDate, List<PurchaseLineDTO> lines
    ) {
        this(id, purchaseNumber, supplierId, supplierName, warehouseId, companyId,
                totalAmount, taxAmount, amountPaid, status, purchaseDate, lines, null, null);
    }
}
