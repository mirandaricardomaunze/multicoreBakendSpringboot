package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AuthSessionService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.model.AppUserCompanyAccess;
import mz.multicore.erp.modules.users.model.UserRole;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;

/** Acessos de uma conta a cada empresa: conceder, mudar o perfil e retirar. */
final class UserCompanyAccessOperations {

    private final AppUserRepository appUserRepository;
    private final CompanyLookup companyLookup;
    private final AppUserCompanyAccessRepository companyAccessRepository;
    private final AuditLogService auditLogService;
    private final AuthSessionService authSessionService;

    UserCompanyAccessOperations(AppUserRepository appUserRepository, CompanyLookup companyLookup,
                                AppUserCompanyAccessRepository companyAccessRepository,
                                AuditLogService auditLogService, AuthSessionService authSessionService) {
        this.appUserRepository = appUserRepository;
        this.companyLookup = companyLookup;
        this.companyAccessRepository = companyAccessRepository;
        this.auditLogService = auditLogService;
        this.authSessionService = authSessionService;
    }

    AppUser grant(AppUser actor, AppUser user, Long companyId, String role) {
        if (actor == null || actor.findCompanyAccess(companyId)
                .filter(access -> "ADMIN".equalsIgnoreCase(access.getRole())).isEmpty()) {
            throw new BusinessRuleException("Só um administrador da empresa de destino pode conceder-lhe acesso.");
        }
        Company company = companyLookup.require(companyId);
        user.grantCompany(company, role);
        return appUserRepository.save(user);
    }

    AppUser updateRole(AppUser user, Long companyId, String role) {
        String normalizedRole = UserRole.normalize(role);
        AppUserCompanyAccess access = user.findCompanyAccess(companyId)
                .orElseThrow(() -> new BusinessRuleException("O utilizador não pertence à empresa ativa."));
        boolean removingAdmin = "ADMIN".equalsIgnoreCase(access.getRole())
                && !"ADMIN".equals(normalizedRole);
        if (removingAdmin && companyAccessRepository.countActiveByCompanyIdAndRole(companyId, "ADMIN") <= 1) {
            throw new BusinessRuleException("A empresa deve manter pelo menos um administrador.");
        }

        String oldRole = access.getRole();
        access.setRole(normalizedRole);
        AppUser saved = appUserRepository.save(user);
        if (!normalizedRole.equalsIgnoreCase(oldRole) && authSessionService != null) {
            authSessionService.revokeUser(user.getUsername());
        }
        audit("USER_ROLE_CHANGE", "Perfil do utilizador " + user.getUsername() + " na empresa " + companyId
                + " alterado de " + oldRole + " para " + normalizedRole);
        return saved;
    }

    /** Numa conta partilhada, desactivar afecta só esta empresa: a pessoa perde o acesso a ela. */
    AppUser removeFromCompany(AppUser user, Long companyId, boolean active) {
        if (active) {
            throw new BusinessRuleException("Esta conta também pertence a outra empresa. "
                    + "Só o administrador da plataforma a pode reactivar.");
        }
        user.revokeCompany(companyId);
        AppUser saved = appUserRepository.save(user);
        if (authSessionService != null) {
            authSessionService.revokeUser(user.getUsername());
        }
        audit("USER_COMPANY_ACCESS_REVOKE", "Removido o acesso de " + user.getUsername() + " à empresa " + companyId);
        return saved;
    }

    private void audit(String action, String details) {
        if (auditLogService != null) {
            auditLogService.logCurrent(action, details);
        }
    }
}
