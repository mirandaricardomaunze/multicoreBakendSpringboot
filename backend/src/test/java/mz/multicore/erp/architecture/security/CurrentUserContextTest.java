package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CF-01..CF-06 do {@code docs/CONTEXTO_FAIL_CLOSED_HARNESS.md}.
 *
 * <p>O contexto é {@code ThreadLocal} e a suite corre numa JVM partilhada
 * ({@code forkCount=1}/{@code reuseForks=true}), por isso limpa-se antes <b>e</b> depois de cada teste
 * para não contaminar as classes seguintes.
 */
class CurrentUserContextTest {

    @BeforeEach
    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    /** CF-01 — sem contexto não há papel. Antes devolvia "ADMIN" e abria o PermissionGuard todo. */
    @Test
    void getRole_semContexto_naoDevolvePapelPrivilegiado() {
        assertEquals("", CurrentUserContext.getRole());
    }

    /** CF-02 — o nome mantém-se como etiqueta de auditoria; é o papel que era perigoso. */
    @Test
    void getUsername_semContexto_continuaSystem() {
        assertEquals("SYSTEM", CurrentUserContext.getUsername());
    }

    /** CF-03 — sem contexto não há empresa. Antes devolvia 1L e lia o tenant errado em silêncio. */
    @Test
    void getCurrentCompanyId_semContexto_lanca() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                CurrentUserContext::getCurrentCompanyId);

        assertEquals("Selecione uma empresa antes de continuar.", ex.getMessage());
    }

    /** CF-04 — as variantes find* são para infra que legitimamente corre sem tenant: nunca lançam. */
    @Test
    void findCurrentUserEFindCurrentCompanyId_semContexto_devolvemNull() {
        assertNull(CurrentUserContext.findCurrentUser());
        assertNull(CurrentUserContext.findCurrentCompanyId());
    }

    /** CF-05 — runAsSystem eleva durante a tarefa e repõe o contexto anterior no fim. */
    @Test
    void runAsSystem_elevaDuranteATarefaERepoeContextoAnterior() {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");
        CurrentUserContext.setCurrentCompanyId(7L);

        String papelDentro = CurrentUserContext.runAsSystem(CurrentUserContext::getRole);

        assertEquals("ADMIN", papelDentro);
        assertEquals("EMPLOYEE", CurrentUserContext.getRole());
        assertEquals("caixa", CurrentUserContext.getUsername());
        assertEquals(7L, CurrentUserContext.getCurrentCompanyId());
    }

    /** CF-05b — a variante com empresa liga o tenant explicitamente, em vez de o deixar adivinhar. */
    @Test
    void runAsSystem_comEmpresa_ligaTenantExplicitoELimpaNoFim() {
        Long empresaDentro = CurrentUserContext.runAsSystem(3L, CurrentUserContext::getCurrentCompanyId);

        assertEquals(3L, empresaDentro);
        assertNull(CurrentUserContext.findCurrentCompanyId());
        assertNull(CurrentUserContext.findCurrentUser());
    }

    /** CF-06 — uma tarefa que rebenta não pode deixar o contexto elevado para trás. */
    @Test
    void runAsSystem_comExcepcao_propagaMasRepoeContexto() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(2L);

        assertThrows(IllegalStateException.class, () -> CurrentUserContext.runAsSystem(() -> {
            throw new IllegalStateException("falha a meio");
        }));

        assertEquals("MANAGER", CurrentUserContext.getRole());
        assertEquals(2L, CurrentUserContext.getCurrentCompanyId());
    }
}
