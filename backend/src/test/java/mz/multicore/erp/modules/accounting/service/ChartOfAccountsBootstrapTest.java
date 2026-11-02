package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.events.CompanyCreatedEvent;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Harness CD-08 e CD-09: nenhuma empresa fica sem plano de contas. */
class ChartOfAccountsBootstrapTest {

    private CompanyRepository companyRepository;
    private AccountRepository accountRepository;
    private ChartOfAccountsBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyRepository.class);
        accountRepository = mock(AccountRepository.class);
        when(accountRepository.findByCompanyIdOrderByCode(anyLong())).thenReturn(List.of());
        bootstrap = new ChartOfAccountsBootstrap(companyRepository, accountRepository,
                new ChartOfAccountsMutationService(accountRepository, mock(CompanyService.class)));
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }

    @SuppressWarnings("unchecked")
    private List<Account> savedAccounts() {
        ArgumentCaptor<List<Account>> saved = ArgumentCaptor.forClass(List.class);
        verify(accountRepository).saveAll(saved.capture());
        return saved.getValue();
    }

    @Test // CD-08
    void empresaNova_recebePlanoCompleto_semUtilizadorDaEmpresa() {
        Company company = company(5L);
        when(companyRepository.getReferenceById(5L)).thenReturn(company);

        bootstrap.onCompanyCreated(new CompanyCreatedEvent(5L));

        List<Account> accounts = savedAccounts();
        assertFalse(accounts.isEmpty());
        assertEquals(PgcNirfChart.accounts().size(), accounts.size());
        assertEquals(company, accounts.get(0).getCompany());
    }

    @Test // CD-09
    void arranque_soCriaPlanoNaEmpresaQueNaoTem() {
        Company withChart = company(1L);
        Company withoutChart = company(2L);
        when(companyRepository.findAll()).thenReturn(List.of(withChart, withoutChart));
        when(accountRepository.existsByCompanyId(1L)).thenReturn(true);

        bootstrap.run(null);

        assertEquals(withoutChart, savedAccounts().get(0).getCompany());
        verify(accountRepository, never()).findByCompanyIdOrderByCode(1L);
    }

    @Test
    void arranque_comPlanosCompletos_naoGrava() {
        when(companyRepository.findAll()).thenReturn(List.of(company(1L)));
        when(accountRepository.existsByCompanyId(1L)).thenReturn(true);

        bootstrap.run(null);

        verify(accountRepository, never()).saveAll(any());
    }
}
