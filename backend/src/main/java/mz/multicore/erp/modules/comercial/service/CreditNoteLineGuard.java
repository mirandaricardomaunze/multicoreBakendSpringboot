package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;

import java.math.BigDecimal;

/**
 * Extension point for other modules to refuse a credit note line: material paid for and already collected by the
 * client, for example ({@code docs/MATERIAL_POR_LEVANTAR_SPEC.md}). The commercial module calls every guard and
 * never depends on the modules that implement them.
 */
public interface CreditNoteLineGuard {

    /**
     * @param requested total quantity of the line asked by this credit note
     * @param creditable quantity still creditable by the commercial rules (sold minus already credited)
     * @throws mz.multicore.erp.architecture.exception.BusinessRuleException to refuse the line
     */
    void check(Invoice invoice, InvoiceLine line, BigDecimal requested, BigDecimal creditable);
}
