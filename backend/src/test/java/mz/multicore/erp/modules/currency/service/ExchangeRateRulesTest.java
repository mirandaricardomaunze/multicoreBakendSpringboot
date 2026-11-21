package mz.multicore.erp.modules.currency.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.company.service.CompanyBaseCurrencyService;
import mz.multicore.erp.modules.currency.dto.CreateExchangeRateRequest;
import mz.multicore.erp.modules.currency.dto.ExchangeRateDTO;
import mz.multicore.erp.modules.currency.model.ExchangeRate;
import mz.multicore.erp.modules.currency.repository.ExchangeRateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FX-02 — moedas aceites, câmbio > 0 com 4 casas, um por empresa, moeda e data (SPEC §3.2). */
class ExchangeRateRulesTest {

    private static final long COMPANY_ID = 7L;
    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    private final ExchangeRateRepository repository = mock(ExchangeRateRepository.class);
    private final CompanyBaseCurrencyService baseCurrency = mock(CompanyBaseCurrencyService.class);
    private final ExchangeRateService service = new ExchangeRateService(repository, baseCurrency);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        when(baseCurrency.baseCurrency(COMPANY_ID)).thenReturn(CurrencyCode.MZN);
        when(repository.save(any(ExchangeRate.class))).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void fx02_criaCambioComQuatroCasas() {
        ExchangeRateDTO created = service.create(COMPANY_ID,
                new CreateExchangeRateRequest("usd", DAY, new BigDecimal("63.9")));

        assertEquals("USD", created.currency());
        assertEquals(DAY, created.rateDate());
        assertEquals(new BigDecimal("63.9000"), created.rate());
        verify(repository).save(any(ExchangeRate.class));
    }

    @Test
    void fx02_soMoedasDaListaFechada() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.create(COMPANY_ID,
                new CreateExchangeRateRequest("GBP", DAY, new BigDecimal("80"))));

        assertThat(error.getMessage()).contains("GBP").contains("MZN, USD, ZAR, EUR");
        verify(repository, never()).save(any());
    }

    @Test
    void fx02_moedaBaseNaoTemCambioNaTabela() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.create(COMPANY_ID,
                new CreateExchangeRateRequest("MZN", DAY, BigDecimal.ONE)));

        assertThat(error.getMessage()).contains("moeda base");
        verify(repository, never()).save(any());
    }

    @Test
    void fx02_cambioTemDeSerPositivo() {
        for (String value : new String[]{"0", "-1.5"}) {
            BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.create(COMPANY_ID,
                    new CreateExchangeRateRequest("USD", DAY, new BigDecimal(value))));
            assertThat(error.getMessage()).contains("maior que zero");
        }
        assertThrows(BusinessRuleException.class, () -> service.create(COMPANY_ID,
                new CreateExchangeRateRequest("USD", DAY, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void fx02_cambioComMaisDeQuatroCasasERecusado() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.create(COMPANY_ID,
                new CreateExchangeRateRequest("ZAR", DAY, new BigDecimal("3.45678"))));

        assertThat(error.getMessage()).contains("4 casas decimais");
        // zeros à direita não contam como casas a mais
        assertEquals(new BigDecimal("3.4500"), service.create(COMPANY_ID,
                new CreateExchangeRateRequest("ZAR", DAY, new BigDecimal("3.450000"))).rate());
    }

    @Test
    void fx02_dataObrigatoria() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.create(COMPANY_ID,
                new CreateExchangeRateRequest("EUR", null, new BigDecimal("70"))));

        assertThat(error.getMessage()).contains("data");
    }

    @Test
    void fx02_umCambioPorEmpresaMoedaEData() {
        when(repository.existsByCompanyIdAndCurrencyAndRateDate(COMPANY_ID, "EUR", DAY)).thenReturn(true);

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.create(COMPANY_ID,
                new CreateExchangeRateRequest("EUR", DAY, new BigDecimal("70"))));

        assertThat(error.getMessage()).contains("EUR").contains("20/11/2026").contains("Corrija");
        verify(repository, never()).save(any());
    }

    @Test
    void fx02_soNaEmpresaActiva() {
        assertThrows(BusinessRuleException.class, () -> service.create(8L,
                new CreateExchangeRateRequest("USD", DAY, new BigDecimal("63.9"))));
        verify(repository, never()).save(any());
    }

    @Test
    void fx02_migracaoCriaTabelaComChaveEmpresaMoedaData() throws IOException {
        assertThat(Files.readString(Path.of("src/main/resources/db/migration/V141__exchange_rates.sql"))).containsIgnoringCase("CREATE TABLE IF NOT EXISTS exchange_rates")
                .containsIgnoringCase("NUMERIC(19, 4)")
                .containsIgnoringCase("UNIQUE (company_id, currency, rate_date)")
                .containsIgnoringCase("CHECK (rate > 0)");
    }
}
