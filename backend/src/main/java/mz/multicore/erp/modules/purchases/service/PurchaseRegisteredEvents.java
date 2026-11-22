package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.events.PurchaseRegisteredEvent;
import mz.multicore.erp.modules.purchases.model.Purchase;

import java.math.BigDecimal;

/**
 * Builds the accounting fact of a purchase in the company's base currency, at the exchange rate snapshotted in the
 * document (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3). Net is derived as base total − base VAT so the
 * journal entry is always balanced after rounding.
 */
final class PurchaseRegisteredEvents {

    private PurchaseRegisteredEvents() {
    }

    /**
     * @param paidNowInFull {@code true} when the purchase is settled at registration (the base total leaves treasury)
     */
    static PurchaseRegisteredEvent of(Purchase purchase, boolean paidNowInFull, boolean cashPayment) {
        BigDecimal baseTotal = purchase.baseTotal();
        BigDecimal baseTax = purchase.toBaseAmount(purchase.getTaxAmount());
        return new PurchaseRegisteredEvent(
                purchase.getCompany().getId(), purchase.getId(), purchase.getPurchaseNumber(),
                purchase.getPurchaseDate().toLocalDate(),
                baseTotal.subtract(baseTax), baseTax, baseTotal,
                paidNowInFull ? baseTotal : BigDecimal.ZERO, cashPayment);
    }
}
