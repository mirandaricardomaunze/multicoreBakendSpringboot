package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreditNoteDTO(
        Long id,
        String noteNumber,
        LocalDateTime issueDate,
        Long companyId,
        Long clientId,
        String clientName,
        Long invoiceId,
        String invoiceNumber,
        Long warehouseId,
        String warehouseName,
        String reason,
        String status,
        BigDecimal totalBeforeTax,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String description,
        String approvedBy,
        LocalDateTime approvedAt,
        String rejectionReason,
        List<CreditNoteLineDTO> lines,
        Long businessUnitId,
        String businessUnitName
) {
    public CreditNoteDTO(Long id, String noteNumber, LocalDateTime issueDate, Long companyId,
                         Long clientId, String clientName, Long invoiceId, String invoiceNumber,
                         Long warehouseId, String warehouseName, String reason, String status,
                         BigDecimal totalBeforeTax, BigDecimal taxAmount, BigDecimal totalAmount,
                         String description, String approvedBy, LocalDateTime approvedAt,
                         String rejectionReason, List<CreditNoteLineDTO> lines) {
        this(id, noteNumber, issueDate, companyId, clientId, clientName, invoiceId, invoiceNumber,
                warehouseId, warehouseName, reason, status, totalBeforeTax, taxAmount, totalAmount,
                description, approvedBy, approvedAt, rejectionReason, lines, null, null);
    }
}
