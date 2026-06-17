package com.phcpro.architecture.security;

import com.phcpro.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
}
