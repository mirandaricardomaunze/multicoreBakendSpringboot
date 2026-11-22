package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nota de débito aprovada, pronta para reconhecer o acréscimo ao cliente.
 *
 * @param warehouseId armazém da fatura corrigida — o proveito fica no seu centro de custo (CC-05); {@code null} usa o
 *                    centro por omissão
 */
public record DebitNoteApprovedEvent(
        Long companyId,
        Long noteId,
        String noteNumber,
        LocalDate date,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Long warehouseId
) {
    /** Nota sem armazém conhecido (centro por omissão). */
    public DebitNoteApprovedEvent(Long companyId, Long noteId, String noteNumber, LocalDate date, BigDecimal netAmount,
                                  BigDecimal taxAmount, BigDecimal totalAmount) {
        this(companyId, noteId, noteNumber, date, netAmount, taxAmount, totalAmount, null);
    }
}
