package mz.multicore.erp.modules.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representação imutável de um Vale de Compras (Store Credit / Voucher) emitido no POS
 * em devoluções ou crédito em loja.
 */
public record StoreVoucherDTO(
        Long id,
        String code,
        BigDecimal initialAmount,
        BigDecimal remainingAmount,
        Long companyId,
        Long clientId,
        String clientName,
        Long creditNoteId,
        String creditNoteNumber,
        LocalDateTime issuedAt,
        LocalDate expiresAt,
        String status,
        String createdBy
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status) || "PARTIALLY_USED".equalsIgnoreCase(status);
    }

    public boolean isExpired() {
        return "EXPIRED".equalsIgnoreCase(status) || (expiresAt != null && expiresAt.isBefore(LocalDate.now()));
    }
}
