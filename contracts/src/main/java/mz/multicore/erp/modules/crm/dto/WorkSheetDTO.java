package mz.multicore.erp.modules.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WorkSheetDTO(
    Long id,
    Long ticketId,
    String subject,
    Long clientId,
    String clientName,
    String technicianName,
    BigDecimal hoursWorked,
    String description,
    String partsUsed,
    BigDecimal partsCost,
    BigDecimal hourlyRate,
    BigDecimal totalValue,
    Boolean isBilled,
    boolean voided,
    String voidReason,
    LocalDateTime createdAt
) {
    /** Rótulo único para a coluna de estado: anulada &gt; faturada &gt; por faturar. */
    public String statusLabel() {
        if (voided) return "Anulada";
        return Boolean.TRUE.equals(isBilled) ? "Faturada" : "Por faturar";
    }
}
