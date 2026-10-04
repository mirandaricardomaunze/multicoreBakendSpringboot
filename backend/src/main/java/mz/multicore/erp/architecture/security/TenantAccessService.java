package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class TenantAccessService {

    private final AppUserRepository appUserRepository;
    private final mz.multicore.erp.modules.subscription.service.SubscriptionService subscriptionService;

    public TenantAccessService(AppUserRepository appUserRepository) {
        this(appUserRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TenantAccessService(AppUserRepository appUserRepository,
                               mz.multicore.erp.modules.subscription.service.SubscriptionService subscriptionService) {
        this.appUserRepository = appUserRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional(readOnly = true)
    public AppUser requireActiveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessRuleException("Utilizador autenticado em falta.");
        }
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador não encontrado."));
        if (!user.isActive()) {
            throw new BusinessRuleException("Utilizador inativo.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public AppUser requireSuperAdmin(String username) {
        AppUser user = requireActiveUser(username);
        if (!user.isPlatformAdmin()) {
            throw new BusinessRuleException("Acesso restrito ao administrador da plataforma.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public AppUser requireAccess(String username, Long companyId) {
        AppUser user = requireActiveUser(username);
        if (!user.hasCompany(companyId)) {
            throw new BusinessRuleException("O utilizador não tem acesso à empresa selecionada.");
        }
        Company company = user.findCompanyAccess(companyId).orElseThrow().getCompany();
        if (!company.isActive() || (subscriptionService != null && !subscriptionService.allowsLogin(companyId))) {
            throw new BusinessRuleException("A empresa está suspensa ou sem assinatura activa.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public List<Company> getAccessibleCompanies(String username) {
        return requireActiveUser(username).getCompanies().stream()
                .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void selectCompany(Long companyId) {
        AppUser user = requireAccess(CurrentUserContext.getUsername(), companyId);
        CurrentUserContext.setCurrentUser(user.getUsername(), user.getRoleForCompany(companyId));
        CurrentUserContext.setCurrentCompanyId(companyId);
    }
}
