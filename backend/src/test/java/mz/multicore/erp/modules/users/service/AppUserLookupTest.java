package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
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

/** MD-R01 — porta de leitura de utilizadores para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). */
class AppUserLookupTest {

    private final AppUserRepository users = mock(AppUserRepository.class);
    private final AppUserCompanyAccessRepository accesses = mock(AppUserCompanyAccessRepository.class);
    private final AppUserLookup lookup = new AppUserLookup(users, accesses);

    private static AppUser user(long id, String username) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    @Test
    void require_existente_devolveUtilizador() {
        AppUser u = user(7L, "maria");
        when(users.findById(7L)).thenReturn(Optional.of(u));

        assertThat(lookup.require(7L)).isSameAs(u);
    }

    @Test
    void require_inexistente_mensagemNormalizada() {
        when(users.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.require(9L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Utilizador não encontrado.");
    }

    @Test
    void nulos_recusamSemTocarNoRepository() {
        assertThatThrownBy(() -> lookup.require(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Utilizador não encontrado.");
        assertThatThrownBy(() -> lookup.requireByUsername(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Utilizador não encontrado.");
        assertThat(lookup.find(null)).isEmpty();
        assertThat(lookup.findByUsername(null)).isEmpty();
        verifyNoInteractions(users, accesses);
    }

    @Test
    void porUsername_delegaEMensagemNormalizada() {
        AppUser u = user(3L, "admin");
        when(users.findByUsername("admin")).thenReturn(Optional.of(u));
        when(users.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThat(lookup.requireByUsername("admin")).isSameAs(u);
        assertThat(lookup.findByUsername("fantasma")).isEmpty();
        assertThatThrownBy(() -> lookup.requireByUsername("fantasma"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Utilizador não encontrado.");
    }

    @Test
    void listas_delegam() {
        List<AppUser> all = List.of(user(1L, "a"), user(2L, "b"));
        List<AppUser> ofCompany = List.of(user(1L, "a"));
        when(users.findAll()).thenReturn(all);
        when(users.findDistinctByCompanyAccessesCompanyIdOrderByName(5L)).thenReturn(ofCompany);

        assertThat(lookup.listAll()).isEqualTo(all);
        assertThat(lookup.listForCompany(5L)).isEqualTo(ofCompany);
    }

    @Test
    void contagensDeAcessos_delegam() {
        when(accesses.countActiveByCompanyIdAndRole(5L, "ADMIN")).thenReturn(2L);
        when(accesses.countByCompanyId(5L)).thenReturn(4L);

        assertThat(lookup.countActiveByCompanyAndRole(5L, "ADMIN")).isEqualTo(2L);
        assertThat(lookup.countAccessesForCompany(5L)).isEqualTo(4L);
    }

    @Test
    void contrato_serviceSoDeLeitura() {
        assertThat(AppUserLookup.class).hasAnnotation(Service.class);
        Transactional tx = AppUserLookup.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();
    }

    @Test
    void escritaAlheia_passaPeloComandoDosUtilizadores() {
        AppUser u = user(10L, "joao");
        List<AppUser> batch = List.of(u);
        when(users.save(u)).thenReturn(u);
        when(users.saveAll(batch)).thenReturn(batch);
        AppUserCommandService commands = new AppUserCommandService(users);

        assertThat(commands.saveUser(u)).isSameAs(u);
        assertThat(commands.saveUsers(batch)).isEqualTo(batch);
        verify(users).save(u);
        verify(users).saveAll(batch);
        assertThat(AppUserCommandService.class).hasAnnotation(Service.class);
        Transactional tx = AppUserCommandService.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
    }
}
