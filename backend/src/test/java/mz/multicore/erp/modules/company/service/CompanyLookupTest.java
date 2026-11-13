package mz.multicore.erp.modules.company.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.TenantAccessService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** MD-C01 — porta de leitura da empresa para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). */
class CompanyLookupTest {

    private final CompanyRepository repository = mock(CompanyRepository.class);
    private final CompanyLookup lookup = new CompanyLookup(repository);

    private static Company company(long id) {
        Company c = new Company();
        c.setId(id);
        return c;
    }

    @Test
    void require_existente_devolveEmpresa() {
        Company c = company(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(c));

        assertThat(lookup.require(7L)).isSameAs(c);
    }

    @Test
    void require_inexistente_mensagemNormalizada() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.require(9L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Empresa não encontrada.");
    }

    @Test
    void require_idNulo_recusaSemTocarNoRepository() {
        assertThatThrownBy(() -> lookup.require(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Empresa não encontrada.");
    }

    @Test
    void requireActive_inexistente_grafiaDeMocambique() {
        when(repository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.requireActive(3L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Empresa activa não encontrada.");
    }

    @Test
    void find_devolveOptional() {
        when(repository.findById(4L)).thenReturn(Optional.empty());

        assertThat(lookup.find(4L)).isEmpty();
        assertThat(lookup.find(null)).isEmpty();
    }

    @Test
    void reference_delegaNaReferenciaJpa() {
        Company c = company(5L);
        when(repository.getReferenceById(5L)).thenReturn(c);

        assertThat(lookup.reference(5L)).isSameAs(c);
    }

    @Test
    void reference_idNulo_recusa() {
        assertThatThrownBy(() -> lookup.reference(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Empresa não encontrada.");
    }

    @Test
    void findAll_eCount_delegam() {
        List<Company> all = List.of(company(1L), company(2L));
        when(repository.findAll()).thenReturn(all);
        when(repository.count()).thenReturn(2L);

        assertThat(lookup.findAll()).isEqualTo(all);
        assertThat(lookup.count()).isEqualTo(2L);
    }

    @Test
    void findByTaxId_delega() {
        Company c = company(6L);
        when(repository.findByTaxId("400123456")).thenReturn(Optional.of(c));

        assertThat(lookup.findByTaxId("400123456")).contains(c);
        assertThat(lookup.findByTaxId(null)).isEmpty();
    }

    @Test
    void lockForUpdate_usaLockPessimista() {
        Company c = company(8L);
        when(repository.lockById(8L)).thenReturn(Optional.of(c));

        assertThat(lookup.lockForUpdate(8L)).isSameAs(c);
        verify(repository).lockById(8L);
    }

    @Test
    void lockForUpdate_inexistente_mensagemNormalizada() {
        when(repository.lockById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.lockForUpdate(8L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Empresa não encontrada.");
    }

    @Test
    void contrato_serviceSoDeLeitura_lockExigeTransaccaoDoChamador() throws NoSuchMethodException {
        assertThat(CompanyLookup.class).hasAnnotation(Service.class);
        Transactional tx = CompanyLookup.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();

        Transactional lock = CompanyLookup.class.getMethod("lockForUpdate", Long.class)
                .getAnnotation(Transactional.class);
        assertThat(lock).isNotNull();
        assertThat(lock.readOnly()).isFalse();
        assertThat(lock.propagation()).isEqualTo(Propagation.MANDATORY);
    }

    @Test
    void escritaAlheia_passaPeloComandoDoCompanyService() {
        Company c = company(10L);
        when(repository.save(c)).thenReturn(c);
        CompanyService service = new CompanyService(repository, mock(TenantAccessService.class));

        assertThat(service.saveCompany(c)).isSameAs(c);
        verify(repository).save(c);
    }
}
