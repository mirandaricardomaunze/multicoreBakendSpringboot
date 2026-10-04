package mz.multicore.erp.architecture.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.monitoring.service.SystemAlertEmailService;
import mz.multicore.erp.modules.monitoring.service.SystemIncidentManager;
import mz.multicore.erp.modules.monitoring.service.TenantMonitoringService;
import mz.multicore.erp.modules.audit.service.ForensicAuditService;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import mz.multicore.erp.modules.backup.service.ScheduledBackupService;
import mz.multicore.erp.modules.subscription.service.SubscriptionService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityAttackSurfaceTest {

    private final AppUserRepository users = mock(AppUserRepository.class);
    private final AppUserCompanyAccessRepository accesses = mock(AppUserCompanyAccessRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final AuthSessionService sessions = new AuthSessionService();
    private final AppUserService userService = new AppUserService(users, passwords, companies, accesses,
            new ManagerPinRateLimiter(3, 5, java.time.Clock.systemUTC()), null, sessions);

    @AfterEach
    void clear() {
        CurrentUserContext.clear();
    }

    @Test
    void adminHasNoFixedAlternativePasswordAndUnknownUserHasSameError() {
        AppUser admin = user("admin", "ADMIN", 1L);
        admin.setPassword(passwords.encode("outra-senha-forte"));
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));

        String wrong = assertThrows(BusinessRuleException.class,
                () -> userService.authenticate("admin", "admin")).getMessage();
        assertThrows(BusinessRuleException.class, () -> userService.authenticate("admin", "password"));
        String unknown = assertThrows(BusinessRuleException.class,
                () -> userService.authenticate("inexistente", "admin")).getMessage();
        assertEquals(wrong, unknown);
    }

    @Test
    void tenantAdminCannotResetPlatformOrOtherTenantAndOwnResetRevokesSessions() {
        CurrentUserContext.setCurrentUser("admin-a", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
        AppUser platform = user("superadmin", "ADMIN", null);
        platform.setPlatformAdmin(true);
        AppUser otherTenant = user("admin-b", "ADMIN", 2L);
        AppUser ownUser = user("operador-a", "SELLER", 1L);
        when(users.findByUsername("superadmin")).thenReturn(Optional.of(platform));
        when(users.findByUsername("admin-b")).thenReturn(Optional.of(otherTenant));
        when(users.findByUsername("operador-a")).thenReturn(Optional.of(ownUser));
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(BusinessRuleException.class, () -> userService.resetPassword("superadmin", "nova-senha"));
        assertThrows(BusinessRuleException.class, () -> userService.resetPassword("admin-b", "nova-senha"));
        assertThrows(BusinessRuleException.class, () -> userService.setManagerPin("admin-b", "1234"));
        verify(users, never()).save(platform);
        verify(users, never()).save(otherTenant);

        String token = sessions.create(ownUser).token();
        userService.resetPassword("operador-a", "nova-senha");
        assertThrows(BusinessRuleException.class, () -> sessions.requireValid(token));
        assertTrue(passwords.matches("nova-senha", ownUser.getPassword()));
    }

    @Test
    void suspendedCompanyOrSubscriptionLosesAccessOnNextRequest() {
        SubscriptionService subscriptions = mock(SubscriptionService.class);
        TenantAccessService tenantAccess = new TenantAccessService(users, subscriptions);
        AppUser user = user("ana", "ADMIN", 1L);
        when(users.findByUsername("ana")).thenReturn(Optional.of(user));
        when(subscriptions.allowsLogin(1L)).thenReturn(true, false);

        assertDoesNotThrow(() -> tenantAccess.requireAccess("ana", 1L));
        assertThrows(BusinessRuleException.class, () -> tenantAccess.requireAccess("ana", 1L));
        user.findCompanyAccess(1L).orElseThrow().getCompany().setActive(false);
        assertThrows(BusinessRuleException.class, () -> tenantAccess.requireAccess("ana", 1L));
    }

    @Test
    void monitoringUsesRealRoleAndCompanyScope() throws Exception {
        TenantAccessService access = mock(TenantAccessService.class);
        SecurityInterceptor interceptor = new SecurityInterceptor(access, sessions);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AppUser seller = user("vendedor", "SELLER", 1L);
        String token = sessions.create(seller).token();
        when(request.getRequestURI()).thenReturn("/api/monitoring/tenants-health");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Company-Id")).thenReturn("1");
        when(access.requireAccess("vendedor", 1L)).thenReturn(seller);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).sendError(eq(403), anyString());
        assertEquals("", CurrentUserContext.getRole());

        CompanyRepository companyRepo = mock(CompanyRepository.class);
        TenantMonitoringService monitoring = new TenantMonitoringService(companyRepo, accesses,
                mock(ForensicAuditService.class), mock(ScheduledBackupService.class),
                mock(SystemIncidentManager.class), mock(SystemAlertEmailService.class));
        Company own = company(1L);
        Company foreign = company(2L);
        when(companyRepo.findById(1L)).thenReturn(Optional.of(own));
        when(companyRepo.findAll()).thenReturn(List.of(own, foreign));
        CurrentUserContext.setCurrentUser("admin-a", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
        assertEquals(1, monitoring.getTenantsHealth().size());
        verify(companyRepo, never()).findAll();
        CurrentUserContext.setCurrentUser("superadmin", "SUPERADMIN");
        CurrentUserContext.setCurrentCompanyId(null);
        assertEquals(2, monitoring.getTenantsHealth().size());
    }

    @Test
    void forwardedIpRequiresDeclaredProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7");
        assertEquals("127.0.0.1", new ClientIpResolver("").resolve(request));
        assertEquals("203.0.113.7", new ClientIpResolver("127.0.0.1").resolve(request));
    }

    @Test
    void forensicSummaryRejectsForeignCompanyForTenantAdmin() {
        ForensicAuditService forensic = new ForensicAuditService(mock(InvoiceRepository.class),
                mock(StockWasteRepository.class), mock(AuditLogRepository.class));
        CurrentUserContext.setCurrentUser("admin-a", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
        assertThrows(BusinessRuleException.class,
                () -> forensic.getForensicSummaryForCompany(2L, null, null, null, null, null));
    }

    private static AppUser user(String username, String role, Long companyId) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setName(username);
        user.setRole(role);
        user.setActive(true);
        if (companyId != null) user.grantCompany(company(companyId), role);
        return user;
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        company.setName("Empresa " + id);
        company.setActive(true);
        return company;
    }
}
