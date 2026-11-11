package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** MD-V01 — porta de leitura de clientes para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). */
class ClientLookupTest {

    private final ClientRepository clients = mock(ClientRepository.class);
    private final ClientLookup lookup = new ClientLookup(clients);

    private static Client client(long id) {
        Client c = new Client();
        c.setId(id);
        return c;
    }

    @Test
    void requireForCompany_existente_devolveCliente() {
        Client c = client(7L);
        when(clients.findByIdAndCompaniesId(7L, 1L)).thenReturn(Optional.of(c));

        assertThat(lookup.requireForCompany(7L, 1L)).isSameAs(c);
    }

    @Test
    void requireForCompany_inexistente_mensagemNormalizada() {
        when(clients.findByIdAndCompaniesId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.requireForCompany(9L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cliente não encontrado.");
    }

    @Test
    void requireForCompany_idNulo_recusaSemTocarNoRepository() {
        assertThatThrownBy(() -> lookup.requireForCompany(null, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cliente não encontrado.");
        verifyNoInteractions(clients);
    }

    @Test
    void require_semEmpresa_mensagemNormalizada() {
        Client c = client(4L);
        when(clients.findById(4L)).thenReturn(Optional.of(c));
        when(clients.findById(5L)).thenReturn(Optional.empty());

        assertThat(lookup.require(4L)).isSameAs(c);
        assertThatThrownBy(() -> lookup.require(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cliente não encontrado.");
    }

    @Test
    void find_devolvemOptional() {
        Client c = client(4L);
        when(clients.findByIdAndCompaniesId(4L, 2L)).thenReturn(Optional.of(c));
        when(clients.findById(4L)).thenReturn(Optional.of(c));

        assertThat(lookup.findForCompany(4L, 2L)).contains(c);
        assertThat(lookup.findForCompany(null, 2L)).isEmpty();
        assertThat(lookup.find(4L)).contains(c);
        assertThat(lookup.find(null)).isEmpty();
    }

    @Test
    void listForCompany_delega() {
        List<Client> all = List.of(client(1L), client(2L));
        when(clients.findDistinctByCompaniesIdOrderByName(3L)).thenReturn(all);

        assertThat(lookup.listForCompany(3L)).isEqualTo(all);
    }

    @Test
    void contrato_serviceSoDeLeitura() {
        assertThat(ClientLookup.class).hasAnnotation(Service.class);
        Transactional tx = ClientLookup.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();
    }

    @Test
    void escritaAlheia_passaPeloComandoDoServiceDeCliente() {
        Client c = client(10L);
        when(clients.save(c)).thenReturn(c);
        ClientCommandService commands = new ClientCommandService(clients);

        assertThat(commands.saveClient(c)).isSameAs(c);
        verify(clients).save(c);
        assertThat(ClientCommandService.class).hasAnnotation(Service.class);
        Transactional tx = ClientCommandService.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
    }
}
