package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.currency.dto.BaseCurrencyDTO;
import mz.multicore.erp.modules.currency.dto.ChangeBaseCurrencyRequest;
import mz.multicore.erp.modules.currency.dto.CorrectExchangeRateRequest;
import mz.multicore.erp.modules.currency.dto.CreateExchangeRateRequest;
import mz.multicore.erp.modules.currency.dto.ExchangeRateDTO;
import mz.multicore.erp.modules.currency.dto.ProposedExchangeRateDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** HTTP client for the exchange-rate table and the base currency of the active company ({@code /api/exchange-rates}). */
@Component
@Profile("desktop")
public class ExchangeRateApiClient {

    private final DesktopClientFactory clientFactory;

    public ExchangeRateApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ExchangeRateDTO> list() {
        return clientFactory.authenticatedClient().getList("/api/exchange-rates", ExchangeRateDTO.class);
    }

    public ExchangeRateDTO create(String currency, LocalDate rateDate, BigDecimal rate) {
        return clientFactory.authenticatedClient().post("/api/exchange-rates",
                new CreateExchangeRateRequest(currency, rateDate, rate), ExchangeRateDTO.class);
    }

    public ExchangeRateDTO correct(Long id, BigDecimal rate) {
        return clientFactory.authenticatedClient().put("/api/exchange-rates/" + id,
                new CorrectExchangeRateRequest(rate), ExchangeRateDTO.class);
    }

    /** Rate proposed for a document of {@code currency} dated {@code date} (SPEC §3.2). */
    public ProposedExchangeRateDTO propose(String currency, LocalDate date) {
        return clientFactory.authenticatedClient().get("/api/exchange-rates/proposal?currency=" + currency
                + "&date=" + date, ProposedExchangeRateDTO.class);
    }

    public BaseCurrencyDTO baseCurrency() {
        return clientFactory.authenticatedClient().get("/api/exchange-rates/base-currency", BaseCurrencyDTO.class);
    }

    public BaseCurrencyDTO changeBaseCurrency(String currency) {
        return clientFactory.authenticatedClient().put("/api/exchange-rates/base-currency",
                new ChangeBaseCurrencyRequest(currency), BaseCurrencyDTO.class);
    }
}
