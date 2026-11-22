package mz.multicore.erp.modules.purchases.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Currency and exchange rate snapshotted on a purchase document (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3):
 * how many base-currency units one unit of {@code currency} was worth when the document was saved.
 */
public record DocumentCurrency(String currency, BigDecimal exchangeRate) {

    /** Scale of every stored exchange rate (4 decimals, as in the exchange-rate table). */
    public static final int RATE_SCALE = 4;

    /** A document in the base currency always has rate 1. */
    public static final BigDecimal BASE_RATE = BigDecimal.ONE.setScale(RATE_SCALE);

    /**
     * Valor nesta moeda convertido para a moeda base a este câmbio (2 casas, HALF_UP). Na moeda base (câmbio 1) o
     * valor passa tal como veio.
     */
    public BigDecimal toBaseAmount(BigDecimal amount) {
        if (exchangeRate.compareTo(BigDecimal.ONE) == 0) return amount;
        return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
    }
}
