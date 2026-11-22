package mz.multicore.erp.modules.currency.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.company.service.CompanyBaseCurrencyService;
import mz.multicore.erp.modules.currency.dto.CorrectExchangeRateRequest;
import mz.multicore.erp.modules.currency.dto.CreateExchangeRateRequest;
import mz.multicore.erp.modules.currency.dto.ExchangeRateDTO;
import mz.multicore.erp.modules.currency.dto.ProposedExchangeRateDTO;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FX-02 — moedas aceites, câmbio > 0 com 4 casas, um por empresa, moeda e data (SPEC §3.2).
 * FX-03 — câmbio proposto = o da data ou o anterior mais recente; sem nenhum, exige câmbio escrito.
 * FX-04 — corrigir um câmbio da tabela não altera documentos já gravados.
 */
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

    // ---- FX-03 — câmbio proposto pela data ou anterior; sem câmbio exige escrita ----

    private static ExchangeRate rate(String currency, LocalDate date, String value) {
        ExchangeRate entity = new ExchangeRate();
        entity.setCompanyId(COMPANY_ID);
        entity.setCurrency(currency);
        entity.setRateDate(date);
        entity.setRate(new BigDecimal(value));
        return entity;
    }

    @Test
    void fx03_propoeCambioDaPropriaData() {
        when(repository.findFirstByCompanyIdAndCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                COMPANY_ID, "USD", DAY)).thenReturn(Optional.of(rate("USD", DAY, "63.9000")));

        ProposedExchangeRateDTO proposed = service.propose(COMPANY_ID, "usd", DAY);

        assertEquals("USD", proposed.currency());
        assertEquals(DAY, proposed.documentDate());
        assertEquals(new BigDecimal("63.9000"), proposed.rate());
        assertEquals(DAY, proposed.rateDate());
        assertThat(proposed.manualRateRequired()).isFalse();
    }

    @Test
    void fx03_propoeCambioDaDataAnteriorMaisRecente() {
        LocalDate previous = DAY.minusDays(3);
        when(repository.findFirstByCompanyIdAndCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                COMPANY_ID, "ZAR", DAY)).thenReturn(Optional.of(rate("ZAR", previous, "3.4500")));

        ProposedExchangeRateDTO proposed = service.propose(COMPANY_ID, "ZAR", DAY);

        assertEquals(new BigDecimal("3.4500"), proposed.rate());
        assertEquals(previous, proposed.rateDate());
        assertThat(proposed.manualRateRequired()).isFalse();
    }

    @Test
    void fx03_semCambioNaTabelaExigeCambioEscrito() {
        when(repository.findFirstByCompanyIdAndCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                COMPANY_ID, "EUR", DAY)).thenReturn(Optional.empty());

        ProposedExchangeRateDTO proposed = service.propose(COMPANY_ID, "EUR", DAY);

        assertThat(proposed.rate()).isNull();
        assertThat(proposed.rateDate()).isNull();
        assertThat(proposed.manualRateRequired()).isTrue();

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.resolveDocumentRate(COMPANY_ID, "EUR", DAY, null));
        assertThat(error.getMessage()).contains("EUR").contains("20/11/2026").contains("Escreva o câmbio");
    }

    @Test
    void fx03_cambioEscritoPrevaleceSobreOProposto() {
        when(repository.findFirstByCompanyIdAndCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                COMPANY_ID, "USD", DAY)).thenReturn(Optional.of(rate("USD", DAY, "63.9000")));

        assertEquals(new BigDecimal("64.1000"),
                service.resolveDocumentRate(COMPANY_ID, "USD", DAY, new BigDecimal("64.1")));
        assertEquals(new BigDecimal("63.9000"),
                service.resolveDocumentRate(COMPANY_ID, "USD", DAY, null));
        assertThrows(BusinessRuleException.class,
                () -> service.resolveDocumentRate(COMPANY_ID, "USD", DAY, BigDecimal.ZERO));
    }

    @Test
    void fx03_moedaBaseTemSempreCambioUm() {
        ProposedExchangeRateDTO proposed = service.propose(COMPANY_ID, "MZN", DAY);

        assertEquals(new BigDecimal("1.0000"), proposed.rate());
        assertThat(proposed.manualRateRequired()).isFalse();
        assertEquals(new BigDecimal("1.0000"),
                service.resolveDocumentRate(COMPANY_ID, "MZN", DAY, new BigDecimal("63.9")));
        verify(repository, never()).findFirstByCompanyIdAndCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                any(), any(), any());
    }

    @Test
    void fx03_dataEEmpresaActivaObrigatorias() {
        assertThrows(BusinessRuleException.class, () -> service.propose(COMPANY_ID, "USD", null));
        assertThrows(BusinessRuleException.class, () -> service.propose(8L, "USD", DAY));
    }

    // ---- FX-04 — corrigir câmbio não altera documentos gravados ----

    @Test
    void fx04_corrigeSoOValorDoCambio() {
        ExchangeRate stored = rate("USD", DAY, "63.9000");
        stored.setId(11L);
        when(repository.findByIdAndCompanyId(11L, COMPANY_ID)).thenReturn(Optional.of(stored));

        ExchangeRateDTO corrected = service.correct(COMPANY_ID, 11L,
                new CorrectExchangeRateRequest(new BigDecimal("64.25")));

        assertEquals(11L, corrected.id());
        assertEquals("USD", corrected.currency());
        assertEquals(DAY, corrected.rateDate());
        assertEquals(new BigDecimal("64.2500"), corrected.rate());
        verify(repository).save(stored);
    }

    @Test
    void fx04_cambioCorrigidoSegueAsRegrasDaTabela() {
        ExchangeRate stored = rate("USD", DAY, "63.9000");
        when(repository.findByIdAndCompanyId(11L, COMPANY_ID)).thenReturn(Optional.of(stored));

        for (String value : new String[]{"0", "-2", "63.12345"}) {
            assertThrows(BusinessRuleException.class, () -> service.correct(COMPANY_ID, 11L,
                    new CorrectExchangeRateRequest(new BigDecimal(value))));
        }
        assertThrows(BusinessRuleException.class, () -> service.correct(COMPANY_ID, 11L,
                new CorrectExchangeRateRequest(null)));
        assertEquals(new BigDecimal("63.9000"), stored.getRate());
        verify(repository, never()).save(any());
    }

    @Test
    void fx04_soCambiosDaEmpresaActiva() {
        when(repository.findByIdAndCompanyId(12L, COMPANY_ID)).thenReturn(Optional.empty());

        BusinessRuleException missing = assertThrows(BusinessRuleException.class, () -> service.correct(
                COMPANY_ID, 12L, new CorrectExchangeRateRequest(new BigDecimal("64"))));
        assertThat(missing.getMessage()).contains("Câmbio não encontrado");
        assertThrows(BusinessRuleException.class, () -> service.correct(8L, 11L,
                new CorrectExchangeRateRequest(new BigDecimal("64"))));
        verify(repository, never()).save(any());
    }

    @Test
    void fx04_documentoGuardaOValorENaoMudaComACorreccao() {
        ExchangeRate stored = rate("USD", DAY, "63.9000");
        when(repository.findFirstByCompanyIdAndCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                COMPANY_ID, "USD", DAY)).thenReturn(Optional.of(stored));
        when(repository.findByIdAndCompanyId(11L, COMPANY_ID)).thenReturn(Optional.of(stored));

        BigDecimal documentRate = service.resolveDocumentRate(COMPANY_ID, "USD", DAY, null);
        service.correct(COMPANY_ID, 11L, new CorrectExchangeRateRequest(new BigDecimal("65")));

        // o documento já gravado guardou o valor (fotografia), não uma referência à linha da tabela
        assertEquals(new BigDecimal("63.9000"), documentRate);
        // documentos novos passam a receber o câmbio corrigido
        assertEquals(new BigDecimal("65.0000"), service.resolveDocumentRate(COMPANY_ID, "USD", DAY, null));
    }

    @Test
    void fx04_nenhumaTabelaReferenciaATabelaDeCambios() throws IOException {
        try (var migrations = Files.list(Path.of("src/main/resources/db/migration"))) {
            assertThat(migrations.filter(file -> file.toString().endsWith(".sql")))
                    .allSatisfy(file -> assertThat(Files.readString(file))
                            .as("documentos guardam o valor do câmbio, nunca uma FK para exchange_rates: %s", file)
                            .doesNotContainPattern("(?i)REFERENCES\\s+exchange_rates"));
        }
    }
}
