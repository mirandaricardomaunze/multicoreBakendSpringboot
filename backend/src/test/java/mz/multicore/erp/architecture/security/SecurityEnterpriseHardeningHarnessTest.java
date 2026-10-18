package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.model.AppUserCompanyAccess;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Harness: Reforço de Segurança Empresarial (PasswordPolicy, Revogação Atómica & Authorities)")
class SecurityEnterpriseHardeningHarnessTest {

    private AppUserRepository appUserRepository;
    private PasswordEncoder passwordEncoder;
    private CompanyRepository companyRepository;
    private AppUserCompanyAccessRepository companyAccessRepository;
    private AuthSessionService authSessionService;
    private TenantAccessService tenantAccessService;
    private AppUserService appUserService;
    private SecurityInterceptor securityInterceptor;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        companyRepository = mock(CompanyRepository.class);
        companyAccessRepository = mock(AppUserCompanyAccessRepository.class);
        authSessionService = mock(AuthSessionService.class);
        tenantAccessService = mock(TenantAccessService.class);

        when(passwordEncoder.encode(any())).thenAnswer(inv -> "hashed:" + inv.getArgument(0));

        appUserService = new AppUserService(
                appUserRepository,
                passwordEncoder,
                companyRepository,
                companyAccessRepository,
                new ManagerPinRateLimiter(3, 5),
                null,
                authSessionService
        );

        securityInterceptor = new SecurityInterceptor(tenantAccessService, authSessionService);

        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SEC-HARD-01: PasswordPolicy rejeita senhas nulas, vazias ou com menos de 8 caracteres")
    void testPasswordPolicyLength() {
        assertThrows(BusinessRuleException.class, () -> PasswordPolicy.validate(null, "antonio"));
        assertThrows(BusinessRuleException.class, () -> PasswordPolicy.validate("", "antonio"));
        assertThrows(BusinessRuleException.class, () -> PasswordPolicy.validate("   ", "antonio"));
        assertThrows(BusinessRuleException.class, () -> PasswordPolicy.validate("Pass1", "antonio"));
        assertThrows(BusinessRuleException.class, () -> PasswordPolicy.validate("Abc1234", "antonio"));
    }

    @Test
    @DisplayName("SEC-HARD-02: PasswordPolicy exige pelo menos uma letra e pelo menos um dígito")
    void testPasswordPolicyLettersAndDigits() {
        // Apenas letras
        BusinessRuleException exLetters = assertThrows(BusinessRuleException.class,
                () -> PasswordPolicy.validate("ApenasLetrasAqui", "antonio"));
        assertThat(exLetters.getMessage()).contains("letra e um número");

        // Apenas números
        BusinessRuleException exDigits = assertThrows(BusinessRuleException.class,
                () -> PasswordPolicy.validate("1234567890", "antonio"));
        assertThat(exDigits.getMessage()).contains("letra e um número");
    }

    @Test
    @DisplayName("SEC-HARD-03: PasswordPolicy rejeita senhas que contenham ou sejam iguais ao username")
    void testPasswordPolicyUsernameInclusion() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> PasswordPolicy.validate("antonio123", "antonio"));
        assertThat(ex.getMessage()).contains("nome de utilizador");

        BusinessRuleException exCase = assertThrows(BusinessRuleException.class,
                () -> PasswordPolicy.validate("ANTONIO2026", "antonio"));
        assertThat(ex.getMessage()).contains("nome de utilizador");
    }

    @Test
    @DisplayName("SEC-HARD-04: PasswordPolicy rejeita senhas triviais da lista negra comum")
    void testPasswordPolicyBlacklist() {
        assertThrows(BusinessRuleException.class, () -> PasswordPolicy.validate("12345678", "user1"));
        assertThrows(BusinessRuleException.class, () -> PasswordPolicy.validate("password", "user1"));
        assertThrows(BusinessRuleException.class, () -> PasswordPolicy.validate("admin123", "user1"));
        assertThrows(BusinessRuleException.class, () -> PasswordPolicy.validate("multicore", "user1"));
    }

    @Test
    @DisplayName("SEC-HARD-05: PasswordPolicy aceita senhas fortes e conformes")
    void testPasswordPolicyValid() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Forte@2026", "antonio"));
        assertDoesNotThrow(() -> PasswordPolicy.validate("Maputo#9876", "antonio"));
        assertDoesNotThrow(() -> PasswordPolicy.validate("Multicore#Segura2026", "antonio"));
    }

    @Test
    @DisplayName("SEC-HARD-06: AppUserService.createUser valida senha com PasswordPolicy antes de codificar")
    void testCreateUserValidatesPassword() {
        Company company = new Company();
        company.setId(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(appUserRepository.findByUsername("novo_user")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        // Senha fraca rejeitada
        assertThrows(BusinessRuleException.class,
                () -> appUserService.createUser("novo_user", "Novo Utilizador", "123", "SELLER"));

        // Senha forte aceite
        AppUser created = appUserService.createUser(
                "novo_user", "Novo Utilizador", "SenhaForte@2026", "SELLER");
        assertNotNull(created);
        verify(passwordEncoder).encode("SenhaForte@2026");
    }

    @Test
    @DisplayName("SEC-HARD-07: AppUserService.resetPassword valida nova senha com PasswordPolicy e revoga sessões antigas")
    void testResetPasswordValidatesAndRevokes() {
        AppUser user = new AppUser();
        user.setUsername("antonio");
        Company company = new Company();
        company.setId(1L);
        user.grantCompany(company, "SELLER");

        when(appUserRepository.findByUsername("antonio")).thenReturn(Optional.of(user));

        // Senha fraca rejeitada
        assertThrows(BusinessRuleException.class,
                () -> appUserService.resetPassword("antonio", "fraca"));

        // Senha forte aceite e revoga sessões
        appUserService.resetPassword("antonio", "NovaSenhaSegura@2026");
        verify(passwordEncoder).encode("NovaSenhaSegura@2026");
        verify(authSessionService).revokeUser("antonio");
    }

    @Test
    @DisplayName("SEC-HARD-08: AppUserService.toggleUserStatus(false) desativa e revoga imediatamente as sessões do utilizador")
    void testToggleUserStatusRevokesSessions() {
        AppUser user = new AppUser();
        user.setUsername("operador");
        user.setActive(true);
        Company company = new Company();
        company.setId(1L);
        user.grantCompany(company, "SELLER");

        when(appUserRepository.findByUsername("operador")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        appUserService.toggleUserStatus("operador", false);

        assertFalse(user.isActive());
        verify(authSessionService).revokeUser("operador");
    }

    @Test
    @DisplayName("SEC-HARD-09: AppUserService.updateCompanyRole revoga sessões ativas quando o perfil na empresa muda")
    void testUpdateCompanyRoleRevokesSessionsOnChange() {
        AppUser user = new AppUser();
        user.setUsername("gerente");
        Company company = new Company();
        company.setId(1L);
        user.grantCompany(company, "MANAGER");

        when(appUserRepository.findByUsername("gerente")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        // Alterar para SELLER (rebaixamento de privilégios)
        appUserService.updateCompanyRole("gerente", "SELLER");

        verify(authSessionService).revokeUser("gerente");
    }

    @Test
    @DisplayName("SEC-HARD-10: SecurityInterceptor enriquece SecurityContext com ROLE_ e limpa no afterCompletion")
    void testSecurityInterceptorEnrichesAndClearsAuthorities() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/comercial/invoices");
        request.addHeader("Authorization", "Bearer valid-token-123");
        request.addHeader("X-Company-Id", "1");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUser user = new AppUser();
        user.setUsername("carlos");
        Company company = new Company();
        company.setId(1L);
        user.grantCompany(company, "ADMIN");

        when(authSessionService.requireValid("valid-token-123"))
                .thenReturn(new AuthSessionService.AuthSession("valid-token-123", "carlos", null, null));
        when(tenantAccessService.requireAccess("carlos", 1L)).thenReturn(user);

        boolean allowed = securityInterceptor.preHandle(request, response, new Object());
        assertTrue(allowed);

        // Verifica authority no SecurityContextHolder
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");

        // Após conclusão do pedido, limpa tanto CurrentUserContext quanto SecurityContext
        securityInterceptor.afterCompletion(request, response, new Object(), null);
        assertNull(CurrentUserContext.findCurrentUser());
        assertEquals("", CurrentUserContext.getRole());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
