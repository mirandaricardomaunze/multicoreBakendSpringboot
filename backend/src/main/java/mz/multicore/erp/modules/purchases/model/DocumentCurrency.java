package mz.multicore.erp.modules.purchases.model;

import java.math.BigDecimal;

/**
 * Currency and exchange rate snapshotted on a purchase document (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3):
 * how many base-currency units one unit of {@code currency} was worth when the document was saved.
 */
public record DocumentCurrency(String currency, BigDecimal exchangeRate) {

    /** Scale of every stored exchange rate (4 decimals, as in the exchange-rate table). */
    public static final int RATE_SCALE = 4;

    /** A document in the base currency always has rate 1. */
    public static final BigDecimal BASE_RATE = BigDecimal.ONE.setScale(RATE_SCALE);
}
