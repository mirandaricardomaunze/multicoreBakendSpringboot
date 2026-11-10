package mz.multicore.erp.architecture.events;

import mz.multicore.erp.modules.accounting.model.JournalSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Stock changed value without a sale or a purchase: a waste, an inventory adjustment, a carcass breakdown, a
 * production batch or the raw material consumed by a production order. The ledger mirrors it in the goods account
 * (3201) so the balance sheet keeps matching the stock ({@code docs/CONTABILIDADE_SPEC.md}).
 *
 * @param valueOut      value that left stock (posted debit {@code CMVMC} / credit goods)
 * @param valueIn       value that entered stock (posted debit goods / credit {@code inCounterpart})
 * @param inCounterpart account credited for {@code valueIn}: {@code CMVMC} for regularisations and
 *                      transformations, {@code VARIACAO_PRODUCAO} for production
 * @param sourceId      id of the document, unique per {@code source}, so a retry never posts twice
 */
public record InventoryValueChangedEvent(Long companyId, JournalSource source, Long sourceId, String reference,
                                         LocalDate date, BigDecimal valueOut, BigDecimal valueIn, String inCounterpart) {}
