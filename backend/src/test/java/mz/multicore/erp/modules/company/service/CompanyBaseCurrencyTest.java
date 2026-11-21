package mz.multicore.erp.modules.company.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FX-01 — moeda base por empresa, MZN por omissão, imutável depois do primeiro movimento (SPEC §3.1). */
class CompanyBaseCurrencyTest {

    private static final long COMPANY_ID = 7L;

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final CompanyMovementProbe purchases = mock(CompanyMovementProbe.class);
    private final CompanyMovementProbe stock = mock(CompanyMovementProbe.class);
    private final CompanyBaseCurrencyService service =
            new CompanyBaseCurrencyService(companyRepository, List.of(purchases, stock));
    private Company company;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        company = new Company();
        company.setId(COMPANY_ID);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void fx01_empresaNovaTemMoedaBaseMzn() {
        assertEquals("MZN", new Company().getBaseCurrency());
        assertEquals(CurrencyCode.MZN, service.baseCurrency(COMPANY_ID));
    }

    @Test
    void fx01_semMovimentosPodeEscolherOutraMoedaBase() {
        assertEquals(CurrencyCode.USD, service.changeBaseCurrency(COMPANY_ID, "usd"));

        assertEquals("USD", company.getBaseCurrency());
        verify(companyRepository).save(company);
    }

    @Test
    void fx01_depoisDoPrimeiroMovimentoNaoMuda() {
        when(stock.hasMovements(COMPANY_ID)).thenReturn(true);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.changeBaseCurrency(COMPANY_ID, "USD"));

        assertThat(error.getMessage()).contains("moeda base").contains("movimentos");
        assertEquals("MZN", company.getBaseCurrency());
        verify(companyRepository, never()).save(any());
    }

    @Test
    void fx01_mesmaMoedaComMovimentosNaoEErro() {
        when(purchases.hasMovements(COMPANY_ID)).thenReturn(true);

        assertEquals(CurrencyCode.MZN, service.changeBaseCurrency(COMPANY_ID, "MZN"));
        verify(companyRepository, never()).save(any());
    }

    @Test
    void fx01_moedaForaDaListaERecusada() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.changeBaseCurrency(COMPANY_ID, "GBP"));

        assertThat(error.getMessage()).contains("GBP").contains("MZN, USD, ZAR, EUR");
        assertThrows(BusinessRuleException.class, () -> service.changeBaseCurrency(COMPANY_ID, " "));
    }

    @Test
    void fx01_soNaEmpresaActiva() {
        assertThrows(BusinessRuleException.class, () -> service.changeBaseCurrency(8L, "USD"));
        verify(companyRepository, never()).findById(8L);
    }

    @Test
    void fx01_migracaoPoeMznNasEmpresasExistentes() throws IOException {
        Path migrations = Path.of("src/main/resources/db/migration");
        String sql;
        try (Stream<Path> files = Files.list(migrations)) {
            Path file = files.filter(p -> p.getFileName().toString().endsWith("__company_base_currency.sql"))
                    .findFirst().orElseThrow();
            sql = Files.readString(file);
        }
        assertThat(sql).containsIgnoringCase("ALTER TABLE companies")
                .containsIgnoringCase("base_currency VARCHAR(3) DEFAULT 'MZN' NOT NULL");
    }
}
