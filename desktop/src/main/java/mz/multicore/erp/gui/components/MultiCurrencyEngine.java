package mz.multicore.erp.gui.components;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Motor canónico de conversão multimoeda e câmbios para o POS e Faturação.
 * Moeda Base: MZN (Metical Moçambicano).
 */
public class MultiCurrencyEngine {

    public enum Currency {
        MZN("Metical", "MT", BigDecimal.ONE),
        USD("Dólar Americano", "$", new BigDecimal("63.83")),
        ZAR("Rand Sul-Africano", "R", new BigDecimal("3.65")),
        EUR("Euro", "€", new BigDecimal("69.50"));

        private final String label;
        private final String symbol;
        private final BigDecimal defaultRateToMzn;

        Currency(String label, String symbol, BigDecimal defaultRateToMzn) {
            this.label = label;
            this.symbol = symbol;
            this.defaultRateToMzn = defaultRateToMzn;
        }

        public String getLabel() { return label; }
        public String getSymbol() { return symbol; }
        public BigDecimal getDefaultRateToMzn() { return defaultRateToMzn; }
    }

    private static final Map<Currency, BigDecimal> CUSTOM_RATES = new EnumMap<>(Currency.class);

    static {
        for (Currency c : Currency.values()) {
            CUSTOM_RATES.put(c, c.getDefaultRateToMzn());
        }
    }

    public static BigDecimal getExchangeRate(Currency currency) {
        if (currency == Currency.MZN) return BigDecimal.ONE;
        return CUSTOM_RATES.getOrDefault(currency, currency.getDefaultRateToMzn());
    }

    public static void setExchangeRate(Currency currency, BigDecimal rate) {
        if (currency != Currency.MZN && rate != null && rate.signum() > 0) {
            CUSTOM_RATES.put(currency, rate);
        }
    }

    /** Converte um valor em Meticais (MZN) para a moeda estrangeira indicada. */
    public static BigDecimal convertToForeign(BigDecimal mznAmount, Currency targetCurrency) {
        if (mznAmount == null || mznAmount.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (targetCurrency == Currency.MZN) return mznAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = getExchangeRate(targetCurrency);
        return mznAmount.divide(rate, 2, RoundingMode.HALF_UP);
    }

    /** Converte um valor em moeda estrangeira para Meticais (MZN). */
    public static BigDecimal convertToMzn(BigDecimal foreignAmount, Currency sourceCurrency) {
        if (foreignAmount == null || foreignAmount.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (sourceCurrency == Currency.MZN) return foreignAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = getExchangeRate(sourceCurrency);
        return foreignAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /** Calcula o troco exato a devolver em Meticais (MZN) quando o cliente paga em moeda estrangeira. */
    public static BigDecimal calculateChangeInMzn(BigDecimal totalMzn, BigDecimal receivedForeign, Currency foreignCurrency) {
        if (totalMzn == null || receivedForeign == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal receivedInMzn = convertToMzn(receivedForeign, foreignCurrency);
        BigDecimal change = receivedInMzn.subtract(totalMzn);
        return change.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /** Formata o montante com o símbolo da moeda correspondente. */
    public static String formatCurrency(BigDecimal amount, Currency currency) {
        if (amount == null) amount = BigDecimal.ZERO;
        String formatted = String.format(Locale.US, "%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
        if (currency == Currency.MZN) {
            return formatted + " MT";
        }
        return currency.getSymbol() + " " + formatted;
    }
}
