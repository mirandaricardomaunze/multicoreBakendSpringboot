package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** "Consumidor Final" sem corrida entre postos (WI-01 a WI-03; carga de 2026-11-07). */
class WalkInClientProviderTest {

    private ClientRepository clients;
    private CompanyRepository companies;
    private WalkInClientProvider provider;
    private final Client walkIn = new Client();

    @BeforeEach
    void setUp() {
        clients = mock(ClientRepository.class);
        companies = mock(CompanyRepository.class);
        provider = new WalkInClientProvider(clients, new CompanyLookup(companies),mock(PlatformTransactionManager.class));
        CurrentUserContext.setCurrentCompanyId(1L);
        walkIn.setTaxId(WalkInClientProvider.WALK_IN_TAX_ID);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test // WI-01
    void jaExiste_naoGravaNada() {
        when(clients.findByTaxIdAndCompaniesId(WalkInClientProvider.WALK_IN_TAX_ID, 1L)).thenReturn(Optional.of(walkIn));

        assertSame(walkIn, provider.getOrCreate());
        verify(clients, never()).saveAndFlush(any());
    }

    @Test // WI-02
    void outroPostoCriouNoMesmoInstante_usaORegistoDeleEmVezDeFalharAVenda() {
        when(clients.findByTaxIdAndCompaniesId(WalkInClientProvider.WALK_IN_TAX_ID, 1L))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(walkIn));
        when(clients.findByTaxId(WalkInClientProvider.WALK_IN_TAX_ID)).thenReturn(Optional.empty());
        when(clients.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("clients_tax_id_key"));

        assertSame(walkIn, provider.getOrCreate());
    }

    @Test // WI-03
    void arranque_garanteOClienteEmTodasAsEmpresas() {
        Company a = new Company();
        a.setId(1L);
        Company b = new Company();
        b.setId(2L);
        when(companies.findAll()).thenReturn(List.of(a, b));
        when(clients.findByTaxIdAndCompaniesId(any(), any())).thenReturn(Optional.empty());
        when(clients.findByTaxId(any())).thenReturn(Optional.empty());

        new WalkInClientBootstrap(new CompanyLookup(companies), provider).run(null);

        verify(clients, times(2)).saveAndFlush(any());
    }

    @Test // WI-04
    void empresaNova_recebeOClienteSoDepoisDoCommit() throws Exception {
        // ensureFor abre uma transacção própria: dentro da transacção que cria a empresa não a vê
        // e o registo da empresa falhava (docs/EMPRESA_CADASTRO_PROFISSIONAL_HARNESS.md CP-30).
        var listener = WalkInClientBootstrap.class.getMethod("onCompanyCreated",
                mz.multicore.erp.architecture.events.CompanyCreatedEvent.class)
                .getAnnotation(org.springframework.transaction.event.TransactionalEventListener.class);

        assertNotNull(listener, "o listener tem de ser transaccional");
        assertEquals(org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT, listener.phase());
        assertTrue(listener.fallbackExecution(), "sem transacção (seeders) corre logo");
    }
}
