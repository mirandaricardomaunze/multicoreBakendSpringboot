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
        String businessUnitName,
        /** Valor devolvido ao fornecedor (notas DF). */
        BigDecimal returnedAmount,
        /** Moeda do documento; os valores acima estão nesta moeda. */
        String currency,
        /** Câmbio gravado no documento (unidades da moeda base por 1 unidade da moeda). */
        BigDecimal exchangeRate,
        /** Total em moeda base = total × câmbio, 2 casas (gravado no documento). */
        BigDecimal baseTotalAmount
) {
    /** Construtor retrocompatível, anterior à moeda estrangeira: moeda base com câmbio 1. */
    public PurchaseDTO(Long id, String purchaseNumber, Long supplierId, String supplierName, Long warehouseId,
                       Long companyId, BigDecimal totalAmount, BigDecimal taxAmount, BigDecimal amountPaid,
                       String status, LocalDateTime purchaseDate, List<PurchaseLineDTO> lines, Long businessUnitId,
                       String businessUnitName, BigDecimal returnedAmount) {
        this(id, purchaseNumber, supplierId, supplierName, warehouseId, companyId, totalAmount, taxAmount, amountPaid,
                status, purchaseDate, lines, businessUnitId, businessUnitName, returnedAmount, null, BigDecimal.ONE,
                totalAmount);
    }

    /** Construtor retrocompatível, anterior às devoluções ao fornecedor. */
    public PurchaseDTO(Long id, String purchaseNumber, Long supplierId, String supplierName, Long warehouseId,
                       Long companyId, BigDecimal totalAmount, BigDecimal taxAmount, BigDecimal amountPaid,
                       String status, LocalDateTime purchaseDate, List<PurchaseLineDTO> lines, Long businessUnitId,
                       String businessUnitName) {
        this(id, purchaseNumber, supplierId, supplierName, warehouseId, companyId, totalAmount, taxAmount, amountPaid,
                status, purchaseDate, lines, businessUnitId, businessUnitName, BigDecimal.ZERO);
    }

    /** O que o fornecedor deve devolver: pago acima do valor que ficou depois das devoluções. */
    public BigDecimal supplierOwes() {
        BigDecimal kept = (totalAmount == null ? BigDecimal.ZERO : totalAmount)
                .subtract(returnedAmount == null ? BigDecimal.ZERO : returnedAmount);
        BigDecimal owed = (amountPaid == null ? BigDecimal.ZERO : amountPaid).subtract(kept);
        return owed.signum() > 0 ? owed : BigDecimal.ZERO;
    }

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
