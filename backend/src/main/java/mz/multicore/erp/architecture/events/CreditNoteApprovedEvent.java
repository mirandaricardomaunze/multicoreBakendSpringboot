package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nota de crédito aprovada, pronta para estornar a venda no razão.
 *
 * @param warehouseId armazém da nota (ou da fatura) — o estorno fica no seu centro de custo (CC-05); {@code null}
 *                    usa o centro por omissão
 */
public record CreditNoteApprovedEvent(
        Long companyId,
        Long noteId,
        String noteNumber,
        LocalDate date,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal returnedGoodsCost,
        Long warehouseId
) {
    /** Nota sem armazém conhecido (centro por omissão). */
    public CreditNoteApprovedEvent(Long companyId, Long noteId, String noteNumber, LocalDate date, BigDecimal netAmount,
                                   BigDecimal taxAmount, BigDecimal totalAmount, BigDecimal returnedGoodsCost) {
        this(companyId, noteId, noteNumber, date, netAmount, taxAmount, totalAmount, returnedGoodsCost, null);
    }
}
