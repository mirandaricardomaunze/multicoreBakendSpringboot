package mz.multicore.erp.architecture.events;

import java.time.LocalDate;

/** Uma venda realizada foi anulada: a contabilidade estorna o lançamento original. */
public record InvoiceCancelledEvent(Long companyId, Long invoiceId, String invoiceNumber, LocalDate date) {}
