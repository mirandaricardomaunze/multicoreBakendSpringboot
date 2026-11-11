package mz.multicore.erp.modules.platform.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PasswordPolicy;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.service.TreasuryAccountProvisioningService;
import mz.multicore.erp.modules.platform.dto.CreatePlatformUserRequest;
import mz.multicore.erp.modules.platform.dto.OnboardCompanyRequest;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;
import mz.multicore.erp.modules.subscription.dto.SaveSubscriptionRequest;
import mz.multicore.erp.modules.subscription.service.SubscriptionService;
import mz.multicore.erp.modules.users.model.UserRole;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Onboards a company in a single transaction: company, first administrator, subscription and the
 * optional bank account. Any failure rolls everything back, so the platform never ends up with a
 * company that has no administrator or no subscription.
 *
 * <p>Each step reuses the service that owns it; this class only orders them and checks the
 * cheap preconditions first, so a typo in the username fails before anything is written.
 */
@Service
public class PlatformCompanyOnboardingService {

    private final PlatformCompanyService companyService;
    private final PlatformUserService userService;
    private final SubscriptionService subscriptionService;
    private final TreasuryAccountProvisioningService treasuryProvisioning;
    private final CompanyLookup companyLookup;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    public PlatformCompanyOnboardingService(PlatformCompanyService companyService,
                                            PlatformUserService userService,
                                            SubscriptionService subscriptionService,
                                            TreasuryAccountProvisioningService treasuryProvisioning,
                                            CompanyLookup companyLookup,
                                            AppUserRepository appUserRepository,
                                            AuditLogService auditLogService) {
        this.companyService = companyService;
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.treasuryProvisioning = treasuryProvisioning;
        this.companyLookup = companyLookup;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PlatformCompanyDTO onboard(OnboardCompanyRequest request) {
        PermissionGuard.requireSuperAdmin("registar uma empresa");
        if (request == null || request.company() == null) {
            throw new BusinessRuleException("Indique os dados da empresa.");
        }
        OnboardCompanyRequest.Administrator admin = request.administrator();
        if (admin == null) {
            throw new BusinessRuleException("Indique o administrador da empresa.");
        }
        String username = admin.username() == null ? "" : admin.username().trim();
        if (appUserRepository.findByUsername(username).isPresent()) {
            throw new BusinessRuleException("O utilizador '" + username + "' já existe. Escolha outro nome.");
        }
        PasswordPolicy.validate(admin.password(), username);

        PlatformCompanyDTO created = companyService.createCompany(request.company());
        Long companyId = created.id();

        userService.createUser(new CreatePlatformUserRequest(
                username, admin.name(), admin.password(), companyId, UserRole.ADMIN.name()));

        SaveSubscriptionRequest subscription = request.subscription() != null
                ? request.subscription()
                : defaultTrial();
        subscriptionService.saveSubscription(companyId, subscription);

        Company company = companyLookup.require(companyId);
        OnboardCompanyRequest.BankAccount bank = request.bankAccount();
        if (bank != null) {
            treasuryProvisioning.openBankAccount(company, bank.bankName(), bank.accountNumber());
        }

        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "PLATFORM_COMPANY_ONBOARD",
                String.format("Empresa '%s' registada com o administrador '%s' e plano %s%s.",
                        created.name(), username, subscription.plan(),
                        bank != null ? " e conta bancária" : ""));
        return companyService.toDto(company);
    }

    static SaveSubscriptionRequest defaultTrial() {
        return new SaveSubscriptionRequest("TRIAL", BigDecimal.ZERO,
                LocalDate.now().plusDays(OnboardCompanyRequest.DEFAULT_TRIAL_DAYS));
    }
}
