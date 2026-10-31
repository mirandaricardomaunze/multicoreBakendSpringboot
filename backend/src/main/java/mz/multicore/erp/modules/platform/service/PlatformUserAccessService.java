package mz.multicore.erp.modules.platform.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.platform.dto.GrantAccessRequest;
import mz.multicore.erp.modules.platform.dto.PlatformUserDTO;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.model.UserRole;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.stereotype.Component;

@Component
public class PlatformUserAccessService {

    private final AppUserRepository appUserRepository;
    private final AppUserCompanyAccessRepository companyAccessRepository;
    private final PlatformUserAccountService accountService;

    public PlatformUserAccessService(AppUserRepository appUserRepository,
                                     AppUserCompanyAccessRepository companyAccessRepository,
                                     PlatformUserAccountService accountService) {
        this.appUserRepository = appUserRepository;
        this.companyAccessRepository = companyAccessRepository;
        this.accountService = accountService;
    }

    public PlatformUserDTO grantAccess(String username, GrantAccessRequest request) {
        AppUser user = accountService.requireUser(username);
        Company company = accountService.requireCompany(request.companyId());
        String role = UserRole.normalize(request.role());
        user.grantCompany(company, role);
        appUserRepository.save(user);
        accountService.audit(username, "PLATFORM_USER_GRANT",
                "Acesso a '" + company.getName() + "' como " + role + ".");
        return accountService.toDto(user);
    }

    public PlatformUserDTO revokeAccess(String username, Long companyId) {
        AppUser user = accountService.requireUser(username);
        Company company = accountService.requireCompany(companyId);
        boolean removingAdmin = user.findCompanyAccess(companyId)
                .map(access -> "ADMIN".equalsIgnoreCase(access.getRole()))
                .orElse(false);
        if (removingAdmin && user.isActive()
                && companyAccessRepository.countActiveByCompanyIdAndRole(companyId, "ADMIN") <= 1) {
            throw new BusinessRuleException("A empresa deve manter pelo menos um administrador.");
        }
        if (!user.revokeCompany(companyId)) {
            throw new BusinessRuleException("O utilizador não tem acesso a essa empresa.");
        }
        appUserRepository.save(user);
        accountService.audit(username, "PLATFORM_USER_REVOKE", "Acesso a '" + company.getName() + "' revogado.");
        return accountService.toDto(user);
    }
}
