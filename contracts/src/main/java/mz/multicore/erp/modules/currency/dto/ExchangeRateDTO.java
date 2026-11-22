package mz.multicore.erp.modules.currency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateDTO(Long id, String currency, LocalDate rateDate, BigDecimal rate) {
}
