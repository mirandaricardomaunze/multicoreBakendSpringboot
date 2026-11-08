package mz.multicore.erp.modules.platform.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.model.CompanyLegalForm;
import mz.multicore.erp.modules.company.model.MozambiqueProvince;
import mz.multicore.erp.modules.company.model.VatRegime;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.platform.dto.CompanyProfile;
import mz.multicore.erp.modules.platform.dto.CreateCompanyRequest;
import mz.multicore.erp.modules.platform.dto.OnboardCompanyRequest;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;
import mz.multicore.erp.modules.subscription.dto.SaveSubscriptionRequest;
import mz.multicore.erp.modules.subscription.model.PlanType;
import mz.multicore.erp.modules.subscription.model.Subscription;
import mz.multicore.erp.modules.subscription.repository.SubscriptionRepository;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Onboarding against the real Spring context and H2 (docs/EMPRESA_CADASTRO_PROFISSIONAL_HARNESS.md).
 * The class is deliberately not {@code @Transactional}: CP-03 must prove that the service's own
 * transaction rolls back, which a test-level transaction would hide.
 */
@SpringBootTest
@ActiveProfiles("test")
class PlatformCompanyOnboardingIntegrationTest {

    @Autowired private PlatformCompanyOnboardingService onboardingService;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private TreasuryAccountRepository treasuryAccountRepository;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentUser("superadmin", "SUPERADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private static CompanyProfile profile() {
        return new CompanyProfile("Mercearia Sol", CompanyLegalForm.PRIVATE_LIMITED, "100987654",
                new BigDecimal("50000.00"), "ALV-2026-114", "47110", VatRegime.NORMAL, "DAF Maputo 1",
                "Polana Cimento", "Maputo", MozambiqueProvince.MAPUTO_CITY, "1100",
                "+258 82 111 2222", "www.sol.co.mz", "Ana Sitoe", "+258 84 333 4444");
    }

    @Test
    @Transactional
    void cp01_registaEmpresaAdministradorTesteGratuitoEContaBancaria() {
        PlatformCompanyDTO dto = onboardingService.onboard(new OnboardCompanyRequest(
                new CreateCompanyRequest("Sol Comércio, Lda", "400777001", "geral@sol.co.mz",
                        "Av. Julius Nyerere 1200", "+258 84 123 4567", profile()),
                new OnboardCompanyRequest.Administrator("sol.admin", "Ana Sitoe", "Sol2026seguro"),
                null,
                new OnboardCompanyRequest.BankAccount("BCI", "0008 0000 1234 5678 101 13")));

        assertEquals("Mercearia Sol", dto.profile().tradeName());
        assertEquals(MozambiqueProvince.MAPUTO_CITY, dto.profile().province());
        assertEquals(1L, dto.userCount());

        Company company = companyRepository.findById(dto.id()).orElseThrow();
        assertEquals("Av. Julius Nyerere 1200, Polana Cimento, 1100 Maputo", company.formattedAddress());

        AppUser admin = appUserRepository.findByUsername("sol.admin").orElseThrow();
        assertEquals("ADMIN", admin.getRole());

        Subscription subscription = subscriptionRepository.findByCompanyId(dto.id()).orElseThrow();
        assertEquals(PlanType.TRIAL, subscription.getPlan());
        assertEquals(LocalDate.now().plusDays(OnboardCompanyRequest.DEFAULT_TRIAL_DAYS), subscription.getValidUntil());

        List<TreasuryAccount> accounts = treasuryAccountRepository.findByCompanyIdOrderByName(dto.id());
        assertTrue(accounts.stream().anyMatch(a -> a.getAccountType() == TreasuryAccountType.BANK
                && "BCI".equals(a.getName()) && a.getAccountNumber().startsWith("0008")));
    }

    @Test
    void cp03_falhaNaAssinaturaDesfazEmpresaEAdministrador() {
        OnboardCompanyRequest request = new OnboardCompanyRequest(
                new CreateCompanyRequest("Rollback, Lda", "400777003", null, null, null),
                new OnboardCompanyRequest.Administrator("rollback.admin", "Rui", "Rollback2026x"),
                new SaveSubscriptionRequest("PLANO_INEXISTENTE", BigDecimal.ZERO, null),
                null);

        assertThrows(BusinessRuleException.class, () -> onboardingService.onboard(request));

        assertTrue(companyRepository.findByTaxId("400777003").isEmpty(), "a empresa não pode ficar gravada");
        assertTrue(appUserRepository.findByUsername("rollback.admin").isEmpty(), "o administrador não pode ficar gravado");
    }
}
