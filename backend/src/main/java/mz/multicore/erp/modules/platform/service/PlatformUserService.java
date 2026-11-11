package mz.multicore.erp.modules.platform.service;

import mz.multicore.erp.architecture.security.AuthSessionService;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.platform.dto.CreatePlatformUserRequest;
import mz.multicore.erp.modules.platform.dto.GrantAccessRequest;
import mz.multicore.erp.modules.platform.dto.PlatformUserDTO;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestão global de utilizadores pelo superadmin — visão de todas as empresas (o
 * {@code AppUserService} é limitado à empresa activa). Activar/desactivar, repor senha, conceder/
 * revogar acesso a empresas e mudar papel. Guardado por SUPERADMIN e auditado.
 */
@Service
public class PlatformUserService {

    private final PlatformUserAccountService accountService;
    private final PlatformUserAccessService accessService;

    @Autowired
    public PlatformUserService(PlatformUserAccountService accountService,
                               PlatformUserAccessService accessService) {
        this.accountService = accountService;
        this.accessService = accessService;
    }

    public PlatformUserService(AppUserRepository appUserRepository,
                               AppUserCompanyAccessRepository companyAccessRepository,
                               CompanyLookup companyLookup,
                               PasswordEncoder passwordEncoder,
                               AuditLogService auditLogService) {
        this(appUserRepository, companyAccessRepository, companyLookup, passwordEncoder,
                auditLogService, null);
    }

    public PlatformUserService(AppUserRepository appUserRepository,
                               AppUserCompanyAccessRepository companyAccessRepository,
                               CompanyLookup companyLookup,
                               PasswordEncoder passwordEncoder,
                               AuditLogService auditLogService,
                               AuthSessionService authSessionService) {
        PlatformUserAccountService account = new PlatformUserAccountService(
                appUserRepository, companyAccessRepository, companyLookup,
                passwordEncoder, auditLogService, authSessionService);
        this.accountService = account;
        this.accessService = new PlatformUserAccessService(
                appUserRepository, companyAccessRepository, account);
    }

    @Transactional(readOnly = true)
    public List<PlatformUserDTO> listUsers() {
        PermissionGuard.requireSuperAdmin("listar utilizadores");
        return accountService.listUsers();
    }

    @Transactional
    public PlatformUserDTO createUser(CreatePlatformUserRequest request) {
        PermissionGuard.requireSuperAdmin("criar um utilizador");
        return accountService.createUser(request);
    }

    @Transactional
    public PlatformUserDTO updateUser(String username, String name) {
        PermissionGuard.requireSuperAdmin("editar um utilizador");
        return accountService.updateUser(username, name);
    }

    @Transactional
    public PlatformUserDTO setUserActive(String username, boolean active) {
        PermissionGuard.requireSuperAdmin("mudar o estado de um utilizador");
        return accountService.setUserActive(username, active);
    }

    @Transactional
    public void resetPassword(String username, String newPassword) {
        PermissionGuard.requireSuperAdmin("repor a senha de um utilizador");
        accountService.resetPassword(username, newPassword);
    }

    @Transactional
    public PlatformUserDTO grantAccess(String username, GrantAccessRequest request) {
        PermissionGuard.requireSuperAdmin("conceder acesso a uma empresa");
        return accessService.grantAccess(username, request);
    }

    @Transactional
    public PlatformUserDTO revokeAccess(String username, Long companyId) {
        PermissionGuard.requireSuperAdmin("revogar acesso a uma empresa");
        return accessService.revokeAccess(username, companyId);
    }
}
