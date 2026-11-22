package mz.multicore.erp.modules.currency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rate proposed for a document date (SPEC §3.2): the rate of that date or of the most recent earlier
 * date. When the table has none, {@code rate} and {@code rateDate} are null and the user must type it.
 */
public record ProposedExchangeRateDTO(String currency,
                                      LocalDate documentDate,
                                      BigDecimal rate,
                                      LocalDate rateDate,
                                      boolean manualRateRequired) {
}
