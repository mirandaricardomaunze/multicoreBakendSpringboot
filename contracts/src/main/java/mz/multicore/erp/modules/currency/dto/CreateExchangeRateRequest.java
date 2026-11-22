package mz.multicore.erp.modules.currency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * New entry of the daily exchange-rate table: {@code rate} units of base currency per 1 {@code currency}.
 * Validation lives in {@code ExchangeRateService} so the messages are the business ones (SPEC §3.2).
 */
public record CreateExchangeRateRequest(String currency, LocalDate rateDate, BigDecimal rate) {
}
