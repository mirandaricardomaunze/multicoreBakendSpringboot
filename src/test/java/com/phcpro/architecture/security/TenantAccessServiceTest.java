package com.phcpro.architecture.security;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantAccessServiceTest {

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final TenantAccessService tenantAccessService = new TenantAccessService(repository);

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void requireAccess_rejectsCompanyOutsideUserMembership() {
        when(repository.findByUsername("maria")).thenReturn(Optional.of(user("maria", "EMPLOYEE", 1L)));

        assertThrows(BusinessRuleException.class, () -> tenantAccessService.requireAccess("maria", 2L));
    }

    @Test
    void interceptorDerivesRoleFromDatabaseAndSelectsAuthorizedCompany() throws Exception {
        TenantAccessService access = mock(TenantAccessService.class);
        AuthSessionService sessions = mock(AuthSessionService.class);
        SecurityInterceptor interceptor = new SecurityInterceptor(access, sessions);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AppUser user = user("ana", "ADMIN", 2L);
        user.grantCompany(user.getCompanies().iterator().next(), "MANAGER");

        when(request.getHeader("Authorization")).thenReturn("Bearer token-ana");
        when(request.getHeader("X-Company-Id")).thenReturn("2");
        when(sessions.requireValid("token-ana")).thenReturn(
                new AuthSessionService.AuthSession("token-ana", "ana", java.time.Instant.now(),
                        java.time.Instant.now().plusSeconds(3600)));
        when(access.requireAccess("ana", 2L)).thenReturn(user);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals("MANAGER", CurrentUserContext.getRole());
        assertEquals(2L, CurrentUserContext.getCurrentCompanyId());
    }

    @Test
    void interceptorRejectsRequestsWithoutIdentity() throws Exception {
        TenantAccessService access = mock(TenantAccessService.class);
        SecurityInterceptor interceptor = new SecurityInterceptor(access, mock(AuthSessionService.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token e empresa são obrigatórios.");
        verify(access, never()).requireAccess(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void selectingCompanyAppliesRoleConfiguredForThatCompany() {
        AppUser user = user("ana", "ADMIN", 1L);
        Company secondCompany = new Company();
        secondCompany.setId(2L);
        secondCompany.setName("Empresa 2");
        user.grantCompany(secondCompany, "EMPLOYEE");
        when(repository.findByUsername("ana")).thenReturn(Optional.of(user));
        CurrentUserContext.setCurrentUser("ana", "ADMIN");

        tenantAccessService.selectCompany(2L);

        assertEquals(2L, CurrentUserContext.getCurrentCompanyId());
        assertEquals("EMPLOYEE", CurrentUserContext.getRole());
    }

    private static AppUser user(String username, String role, Long companyId) {
        Company company = new Company();
        company.setId(companyId);
        company.setName("Empresa " + companyId);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setName(username);
        user.setRole(role);
        user.setActive(true);
        user.grantCompany(company, role);
        return user;
    }
}
