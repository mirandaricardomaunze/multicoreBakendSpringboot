package mz.multicore.erp.modules.platform.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.service.TreasuryAccountProvisioningService;
import mz.multicore.erp.modules.platform.dto.CreateCompanyRequest;
import mz.multicore.erp.modules.platform.dto.CreatePlatformUserRequest;
import mz.multicore.erp.modules.platform.dto.OnboardCompanyRequest;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;
import mz.multicore.erp.modules.subscription.dto.SaveSubscriptionRequest;
import mz.multicore.erp.modules.subscription.service.SubscriptionService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Ordering and preconditions of {@link PlatformCompanyOnboardingService}; collaborators mocked. */
class PlatformCompanyOnboardingServiceTest {

    private PlatformCompanyService companyService;
    private PlatformUserService userService;
    private SubscriptionService subscriptionService;
    private TreasuryAccountProvisioningService treasury;
    private CompanyRepository companyRepository;
    private AppUserRepository appUserRepository;
    private PlatformCompanyOnboardingService service;

    @BeforeEach
    void setUp() {
        companyService = mock(PlatformCompanyService.class);
        userService = mock(PlatformUserService.class);
        subscriptionService = mock(SubscriptionService.class);
        treasury = mock(TreasuryAccountProvisioningService.class);
        companyRepository = mock(CompanyRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        service = new PlatformCompanyOnboardingService(companyService, userService, subscriptionService,
                treasury, new CompanyLookup(companyRepository), appUserRepository, mock(AuditLogService.class));
        CurrentUserContext.setCurrentUser("superadmin", "SUPERADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private static OnboardCompanyRequest request(String username, String password,
                                                 SaveSubscriptionRequest subscription,
                                                 OnboardCompanyRequest.BankAccount bank) {
        return new OnboardCompanyRequest(
                new CreateCompanyRequest("Nova, Lda", "400123456", null, null, null),
                new OnboardCompanyRequest.Administrator(username, "Ana", password), subscription, bank);
    }

    private void companyIsCreatedWithId(long id) {
        PlatformCompanyDTO created = new PlatformCompanyDTO(id, "Nova, Lda", "400123456", null, null, null,
                false, true, 0, null);
        when(companyService.createCompany(any())).thenReturn(created);
        Company company = new Company();
        company.setId(id);
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(companyService.toDto(company)).thenReturn(created);
    }

    @Test
    void cp02_utilizadorJaExistente_falhaAntesDeCriarAEmpresa() {
        when(appUserRepository.findByUsername("ana")).thenReturn(Optional.of(new AppUser()));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.onboard(request("ana", "Segura2026x", null, null)));

        assertTrue(ex.getMessage().contains("já existe"));
        verifyNoInteractions(companyService, userService, subscriptionService, treasury);
    }

    @Test
    void cp04_senhaFraca_falhaAntesDeCriarAEmpresa() {
        when(appUserRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> service.onboard(request("ana", "123", null, null)));

        verifyNoInteractions(companyService, userService, subscriptionService, treasury);
    }

    @Test
    void cp05_semSuperadmin_bloqueia() {
        CurrentUserContext.setCurrentUser("gerente", "ADMIN");

        assertThrows(BusinessRuleException.class, () -> service.onboard(request("ana", "Segura2026x", null, null)));

        verifyNoInteractions(companyService, appUserRepository);
    }

    @Test
    void cp06_criaAdministradorComPapelAdminETesteDe30Dias() {
        when(appUserRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        companyIsCreatedWithId(42L);

        service.onboard(request(" ana ", "Segura2026x", null, null));

        ArgumentCaptor<CreatePlatformUserRequest> user = ArgumentCaptor.forClass(CreatePlatformUserRequest.class);
        verify(userService).createUser(user.capture());
        assertEquals("ana", user.getValue().username());
        assertEquals(42L, user.getValue().companyId());
        assertEquals("ADMIN", user.getValue().companyRole());

        ArgumentCaptor<SaveSubscriptionRequest> sub = ArgumentCaptor.forClass(SaveSubscriptionRequest.class);
        verify(subscriptionService).saveSubscription(eq(42L), sub.capture());
        assertEquals("TRIAL", sub.getValue().plan());
        assertEquals(LocalDate.now().plusDays(30), sub.getValue().validUntil());
        verifyNoInteractions(treasury);
    }

    @Test
    void cp07_planoEscolhidoEContaBancariaSaoAplicados() {
        when(appUserRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        companyIsCreatedWithId(7L);
        SaveSubscriptionRequest pro = new SaveSubscriptionRequest("PRO", new BigDecimal("2500"), LocalDate.now().plusYears(1));

        service.onboard(request("ana", "Segura2026x", pro,
                new OnboardCompanyRequest.BankAccount("Millennium BIM", "123456789")));

        verify(subscriptionService).saveSubscription(7L, pro);
        verify(treasury).openBankAccount(argThat(c -> c.getId() == 7L), eq("Millennium BIM"), eq("123456789"));
    }
}
