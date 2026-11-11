package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AuthSessionService;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PasswordPolicy;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.model.AppUserCompanyAccess;
import mz.multicore.erp.modules.users.model.UserRole;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administração de contas na empresa activa: listagem, criação, nome, acessos multiempresa,
 * perfil, reposição de senha e activação. Garante sempre pelo menos um administrador activo e
 * revoga as sessões quando o perfil, a senha ou o estado mudam.
 */
@Service
public class UserAccountAdministrationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyLookup companyLookup;
    private final AppUserCompanyAccessRepository companyAccessRepository;
    private final AuditLogService auditLogService;
    private final AuthSessionService authSessionService;
    private final UserCompanyAccessOperations companyAccess;

    public UserAccountAdministrationService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                                            CompanyLookup companyLookup,
                                            AppUserCompanyAccessRepository companyAccessRepository,
                                            AuditLogService auditLogService,
                                            AuthSessionService authSessionService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.companyLookup = companyLookup;
        this.companyAccessRepository = companyAccessRepository;
        this.auditLogService = auditLogService;
        this.authSessionService = authSessionService;
        this.companyAccess = new UserCompanyAccessOperations(appUserRepository, companyLookup,
                companyAccessRepository, auditLogService, authSessionService);
    }

    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        UserAdminGuard.requireAdmin();
        return appUserRepository.findDistinctByCompanyAccessesCompanyIdOrderByName(
                CurrentUserContext.getCurrentCompanyId());
    }

    @Transactional
    public AppUser createUser(String username, String name, String password, String role) {
        UserAdminGuard.requireAdmin();
        PasswordPolicy.validate(password, username);
        if (appUserRepository.findByUsername(username).isPresent()) {
            throw new BusinessRuleException("Utilizador já existe.");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordChangeRequired(true);
        String normalizedRole = UserRole.normalize(role);
        user.setRole(normalizedRole);
        user.setActive(true);
        Company company = companyLookup.requireActive(CurrentUserContext.getCurrentCompanyId());
        user.grantCompany(company, normalizedRole);
        user.setCreatedBy(CurrentUserContext.getUsername());

        AppUser saved = appUserRepository.save(user);
        audit("USER_CREATE", "Criado utilizador " + username + " com perfil " + normalizedRole);
        return saved;
    }

    @Transactional
    public AppUser updateUserName(String username, String name) {
        UserAdminGuard.requireAdmin();
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome do utilizador é obrigatório.");
        }
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        AppUser user = findUser(username);
        if (user.findCompanyAccess(companyId).isEmpty()) {
            throw new BusinessRuleException("O utilizador não pertence à empresa ativa.");
        }
        SharedAccountGuard.requireControlledAccount(user, currentActor(), companyId);
        user.setName(name.trim());
        return appUserRepository.save(user);
    }

    @Transactional
    public AppUser grantCompanyAccess(String username, Long companyId, String role) {
        UserAdminGuard.requireAdmin();
        return companyAccess.grant(currentActor(), findUser(username), companyId, role);
    }

    @Transactional
    public AppUser updateCompanyRole(String username, String role) {
        UserAdminGuard.requireAdmin();
        return companyAccess.updateRole(findUser(username), CurrentUserContext.requireCurrentCompanyId(), role);
    }

    @Transactional
    public void resetPassword(String username, String newPassword) {
        UserAdminGuard.requireAdmin();
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        PasswordPolicy.validate(newPassword, username);
        AppUser user = findUser(username);
        UserAdminGuard.requireManagedUser(user, companyId);
        SharedAccountGuard.requireControlledAccount(user, currentActor(), companyId);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(true);
        appUserRepository.save(user);
        if (authSessionService != null) {
            authSessionService.revokeUser(user.getUsername());
        }
        audit("USER_PASSWORD_RESET", "Reposta senha do utilizador " + username);
    }

    @Transactional
    public AppUser toggleUserStatus(String username, boolean active) {
        UserAdminGuard.requireAdmin();
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        AppUser user = findUser(username);
        AppUserCompanyAccess access = requireAccess(user, companyId);
        if (!active && user.isActive() && "ADMIN".equalsIgnoreCase(access.getRole())
                && companyAccessRepository.countActiveByCompanyIdAndRole(companyId, "ADMIN") <= 1) {
            throw new BusinessRuleException("Não é possível desativar o único administrador da empresa.");
        }
        if (!SharedAccountGuard.controlsAccount(user, currentActor(), companyId)) {
            return companyAccess.removeFromCompany(user, companyId, active);
        }
        user.setActive(active);
        AppUser saved = appUserRepository.save(user);
        if (!active && authSessionService != null) {
            authSessionService.revokeUser(user.getUsername());
        }
        audit("USER_STATUS_CHANGE", "Estado do utilizador " + username + " alterado para "
                + (active ? "ATIVO" : "INATIVO"));
        return saved;
    }

    private AppUser currentActor() {
        return appUserRepository.findByUsername(CurrentUserContext.getUsername()).orElse(null);
    }

    private AppUser findUser(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador não encontrado."));
    }

    private static AppUserCompanyAccess requireAccess(AppUser user, Long companyId) {
        return user.findCompanyAccess(companyId)
                .orElseThrow(() -> new BusinessRuleException("O utilizador não pertence à empresa ativa."));
    }

    private void audit(String action, String details) {
        if (auditLogService != null) {
            auditLogService.logCurrent(action, details);
        }
    }
}
