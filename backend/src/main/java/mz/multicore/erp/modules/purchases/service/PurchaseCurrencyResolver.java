package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.currency.service.ExchangeRateService;
import mz.multicore.erp.modules.purchases.model.DocumentCurrency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Decides the currency and exchange rate snapshotted on a purchase order or purchase
 * (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3): no currency means the company's base currency, which always
 * has rate 1; a foreign currency takes the typed rate or the one proposed by the exchange-rate table.
 */
@Component
public class PurchaseCurrencyResolver {

    /** Absent only in wiring without the exchange-rate table, where foreign-currency documents are refused. */
    private final ExchangeRateService exchangeRateService;

    public PurchaseCurrencyResolver(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    /** Resolver for wiring without the exchange-rate table: only base-currency documents are accepted. */
    public static PurchaseCurrencyResolver baseCurrencyOnly() {
        return new PurchaseCurrencyResolver(null);
    }

    public DocumentCurrency resolve(Company company, String requestedCurrency, LocalDate documentDate,
                                    BigDecimal typedRate) {
        CurrencyCode base = CurrencyCode.parse(company.getBaseCurrency());
        CurrencyCode currency = requestedCurrency == null || requestedCurrency.isBlank()
                ? base : CurrencyCode.parse(requestedCurrency);
        if (currency == base) {
            return new DocumentCurrency(base.name(), DocumentCurrency.BASE_RATE);
        }
        if (exchangeRateService == null) {
            throw new BusinessRuleException("Compras em " + currency.name() + " não estão disponíveis neste contexto.");
        }
        return new DocumentCurrency(currency.name(),
                exchangeRateService.resolveDocumentRate(company.getId(), currency.name(), documentDate, typedRate));
    }
}
