package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Nota de devolução ao fornecedor (série interna DF): abate a dívida, o stock e o IVA dedutível. */
public record SupplierReturnDTO(
        Long id,
        String returnNumber,
        Long purchaseId,
        String purchaseNumber,
        String supplierName,
        LocalDateTime returnDate,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String reason
) {}
