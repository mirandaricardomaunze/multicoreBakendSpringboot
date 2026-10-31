package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.security.AuthSessionService;
import mz.multicore.erp.architecture.security.ManagerPinRateLimiter;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.users.dto.UserSecurityRequestsDTOs.VerifyManagerPinResponse;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fachada da gestão de utilizadores. Delega em {@link UserAuthenticationService},
 * {@link ManagerPinService} e {@link UserAccountAdministrationService}.
 * Ver {@code docs/APP_USER_SERVICE_DECOMPOSITION_SPEC.md}.
 */
@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final UserAuthenticationService authenticationService;
    private final ManagerPinService managerPinService;
    private final UserAccountAdministrationService accountAdministrationService;

    @Autowired
    public AppUserService(AppUserRepository appUserRepository,
                          UserAuthenticationService authenticationService,
                          ManagerPinService managerPinService,
                          UserAccountAdministrationService accountAdministrationService) {
        this.appUserRepository = appUserRepository;
        this.authenticationService = authenticationService;
        this.managerPinService = managerPinService;
        this.accountAdministrationService = accountAdministrationService;
    }

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                          CompanyRepository companyRepository,
                          AppUserCompanyAccessRepository companyAccessRepository) {
        this(appUserRepository, passwordEncoder, companyRepository, companyAccessRepository,
                new ManagerPinRateLimiter(3, 5), null);
    }

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                          CompanyRepository companyRepository,
                          AppUserCompanyAccessRepository companyAccessRepository,
                          ManagerPinRateLimiter managerPinRateLimiter) {
        this(appUserRepository, passwordEncoder, companyRepository, companyAccessRepository,
                managerPinRateLimiter, null);
    }

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                          CompanyRepository companyRepository,
                          AppUserCompanyAccessRepository companyAccessRepository,
                          ManagerPinRateLimiter managerPinRateLimiter,
                          AuditLogService auditLogService) {
        this(appUserRepository, passwordEncoder, companyRepository, companyAccessRepository,
                managerPinRateLimiter, auditLogService, null);
    }

    /** Construtor retrocompatível completo (testes de segurança e auditoria). */
    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                          CompanyRepository companyRepository,
                          AppUserCompanyAccessRepository companyAccessRepository,
                          ManagerPinRateLimiter managerPinRateLimiter,
                          AuditLogService auditLogService,
                          AuthSessionService authSessionService) {
        this(appUserRepository,
                new UserAuthenticationService(appUserRepository, passwordEncoder),
                new ManagerPinService(appUserRepository, passwordEncoder, managerPinRateLimiter, auditLogService),
                new UserAccountAdministrationService(appUserRepository, passwordEncoder, companyRepository,
                        companyAccessRepository, auditLogService, authSessionService));
    }

    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        return accountAdministrationService.getAllUsers();
    }

    @Transactional(readOnly = true)
    public AppUser findByUsername(String username) {
        return appUserRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public AppUser authenticate(String username, String password) {
        return authenticationService.authenticate(username, password);
    }

    @Transactional
    public AppUser createUser(String username, String name, String password, String role) {
        return accountAdministrationService.createUser(username, name, password, role);
    }

    @Transactional
    public AppUser updateUserName(String username, String name) {
        return accountAdministrationService.updateUserName(username, name);
    }

    @Transactional
    public AppUser grantCompanyAccess(String username, Long companyId) {
        return grantCompanyAccess(username, companyId, "EMPLOYEE");
    }

    @Transactional
    public AppUser grantCompanyAccess(String username, Long companyId, String role) {
        return accountAdministrationService.grantCompanyAccess(username, companyId, role);
    }

    @Transactional
    public AppUser updateCompanyRole(String username, String role) {
        return accountAdministrationService.updateCompanyRole(username, role);
    }

    @Transactional
    public void setManagerPin(String username, String pin) {
        managerPinService.setManagerPin(username, pin);
    }

    @Transactional(readOnly = true)
    public VerifyManagerPinResponse verifyManagerPin(String pin) {
        return verifyManagerPin(pin, "local");
    }

    @Transactional(readOnly = true)
    public VerifyManagerPinResponse verifyManagerPin(String pin, String clientIp) {
        return managerPinService.verifyManagerPin(pin, clientIp);
    }

    @Transactional
    public void resetPassword(String username, String newPassword) {
        accountAdministrationService.resetPassword(username, newPassword);
    }

    @Transactional
    public AppUser toggleUserStatus(String username, boolean active) {
        return accountAdministrationService.toggleUserStatus(username, active);
    }
}
