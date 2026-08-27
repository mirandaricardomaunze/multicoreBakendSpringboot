package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionGuardTest {

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void requireManagerOrAdmin_comEmployee_lancaBusinessRuleException() {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");

        assertThrows(BusinessRuleException.class,
                () -> PermissionGuard.requireManagerOrAdmin("anular fatura"));
    }

    @Test
    void requireManagerOrAdmin_comManager_permite() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");

        assertDoesNotThrow(() -> PermissionGuard.requireManagerOrAdmin("anular fatura"));
    }

    @Test
    void requireAdmin_comManager_lancaBusinessRuleException() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");

        assertThrows(BusinessRuleException.class,
                () -> PermissionGuard.requireAdmin("alterar permissões"));
    }

    @Test
    void requireAdmin_comAdmin_permite() {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");

        assertDoesNotThrow(() -> PermissionGuard.requireAdmin("alterar permissões"));
    }

    /**
     * CF-07 — sem contexto a guarda recusa. Antes o {@code CurrentUserContext} inventava o papel
     * "ADMIN", o que tornava esta guarda — a única verificação de papel do sistema — um no-op em
     * qualquer thread sem sessão.
     */
    @Test
    void requireAdmin_semContexto_lancaBusinessRuleException() {
        assertThrows(BusinessRuleException.class,
                () -> PermissionGuard.requireAdmin("gerar cópia de segurança"));
    }

    /** CF-08 — o mesmo pela via booleana, usada para esconder/mostrar acções. */
    @Test
    void isManagerOrAdmin_semContexto_devolveFalse() {
        assertFalse(PermissionGuard.isManagerOrAdmin());
    }
}
