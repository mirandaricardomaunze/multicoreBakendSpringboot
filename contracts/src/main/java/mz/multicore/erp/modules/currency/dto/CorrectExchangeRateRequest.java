package mz.multicore.erp.modules.currency.dto;

import java.math.BigDecimal;

/**
 * Correction of an existing exchange rate: only the value changes, currency and date are its key.
 * Documents already saved keep the rate they stored (SPEC §3.2).
 */
public record CorrectExchangeRateRequest(BigDecimal rate) {
}
