package mz.multicore.erp.modules.currency.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Daily exchange rate of a company (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.2):
 * how many units of the company's base currency one unit of {@link #currency} is worth.
 */
@Entity
@Table(name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(name = "uk_exchange_rates_company_currency_date",
                columnNames = {"company_id", "currency", "rate_date"}))
@Getter
@Setter
public class ExchangeRate extends BaseEntity {

    /** Decimal places of every stored rate. */
    public static final int RATE_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** ISO 4217 code from {@code CurrencyCode}, never the company's base currency. */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "rate", nullable = false, precision = 19, scale = RATE_SCALE)
    private BigDecimal rate;
}
