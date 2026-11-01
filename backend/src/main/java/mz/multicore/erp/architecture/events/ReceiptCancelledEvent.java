package mz.multicore.erp.architecture.events;

import java.time.LocalDate;

/** Um recibo foi anulado: a contabilidade estorna o recebimento original. */
public record ReceiptCancelledEvent(Long companyId, Long receiptId, String receiptNumber, LocalDate date) {}
