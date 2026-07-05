package com.phcpro.modules.platform.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.platform.dto.CreatePlatformUserRequest;
import com.phcpro.modules.platform.dto.GrantAccessRequest;
import com.phcpro.modules.platform.dto.PlatformUserDTO;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.model.AppUserCompanyAccess;
import com.phcpro.modules.users.model.UserRole;
import com.phcpro.modules.users.repository.AppUserCompanyAccessRepository;
import com.phcpro.modules.users.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Gestão global de utilizadores pelo superadmin — visão de todas as empresas (o
 * {@code AppUserService} é limitado à empresa activa). Activar/desactivar, repor senha, conceder/
 * revogar acesso a empresas e mudar papel. Guardado por SUPERADMIN e auditado.
 */
@Service
public class PlatformUserService {

    private final AppUserRepository appUserRepository;
    private final AppUserCompanyAccessRepository companyAccessRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public PlatformUserService(AppUserRepository appUserRepository,
                               AppUserCompanyAccessRepository companyAccessRepository,
                               CompanyRepository companyRepository,
                               PasswordEncoder passwordEncoder,
                               AuditLogService auditLogService) {
        this.appUserRepository = appUserRepository;
        this.companyAccessRepository = companyAccessRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PlatformUserDTO> listUsers() {
        PermissionGuard.requireSuperAdmin("listar utilizadores");
        return appUserRepository.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PlatformUserDTO createUser(CreatePlatformUserRequest request) {
        PermissionGuard.requireSuperAdmin("criar um utilizador");
        appUserRepository.findByUsername(request.username()).ifPresent(u -> {
            throw new BusinessRuleException("Utilizador já existe.");
        });
        Company company = requireCompany(request.companyId());
        String role = UserRole.normalize(request.companyRole());

        AppUser user = new AppUser();
        user.setUsername(request.username().trim());
        user.setName(request.name().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setActive(true);
        user.grantCompany(company, role);
        user.setCreatedBy(CurrentUserContext.getUsername());
        appUserRepository.save(user);

        audit(request.username(), "PLATFORM_USER_CREATE",
                "Utilizador criado e ligado a '" + company.getName() + "' como " + role + ".");
        return toDto(user);
    }

    @Transactional
    public PlatformUserDTO setUserActive(String username, boolean active) {
        PermissionGuard.requireSuperAdmin("mudar o estado de um utilizador");
        AppUser user = requireUser(username);
        if (user.isPlatformAdmin() && !active) {
            throw new BusinessRuleException("Não é possível desactivar o administrador da plataforma.");
        }
        user.setActive(active);
        appUserRepository.save(user);
        audit(username, "PLATFORM_USER_STATUS", (active ? "Activado" : "Desactivado") + " o utilizador.");
        return toDto(user);
    }

    @Transactional
    public void resetPassword(String username, String newPassword) {
        PermissionGuard.requireSuperAdmin("repor a senha de um utilizador");
        if (newPassword == null || newPassword.trim().length() < 4) {
            throw new BusinessRuleException("A nova senha deve ter pelo menos 4 caracteres.");
        }
        AppUser user = requireUser(username);
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        appUserRepository.save(user);
        audit(username, "PLATFORM_USER_PASSWORD", "Senha reposta pelo superadmin.");
    }

    @Transactional
    public PlatformUserDTO grantAccess(String username, GrantAccessRequest request) {
        PermissionGuard.requireSuperAdmin("conceder acesso a uma empresa");
        AppUser user = requireUser(username);
        Company company = requireCompany(request.companyId());
        String role = UserRole.normalize(request.role());
        user.grantCompany(company, role);
        appUserRepository.save(user);
        audit(username, "PLATFORM_USER_GRANT",
                "Acesso a '" + company.getName() + "' como " + role + ".");
        return toDto(user);
    }

    @Transactional
    public PlatformUserDTO revokeAccess(String username, Long companyId) {
        PermissionGuard.requireSuperAdmin("revogar acesso a uma empresa");
        AppUser user = requireUser(username);
        Company company = requireCompany(companyId);
        boolean removingAdmin = user.findCompanyAccess(companyId)
                .map(access -> "ADMIN".equalsIgnoreCase(access.getRole()))
                .orElse(false);
        if (removingAdmin && companyAccessRepository.countByCompanyIdAndRoleIgnoreCase(companyId, "ADMIN") <= 1) {
            throw new BusinessRuleException("A empresa deve manter pelo menos um administrador.");
        }
        if (!user.revokeCompany(companyId)) {
            throw new BusinessRuleException("O utilizador não tem acesso a essa empresa.");
        }
        appUserRepository.save(user);
        audit(username, "PLATFORM_USER_REVOKE", "Acesso a '" + company.getName() + "' revogado.");
        return toDto(user);
    }

    private Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
    }

    private AppUser requireUser(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador não encontrado."));
    }

    private void audit(String username, String action, String details) {
        auditLogService.logEvent(CurrentUserContext.getUsername(), null, action,
                "[" + username + "] " + details);
    }

    private PlatformUserDTO toDto(AppUser user) {
        List<PlatformUserDTO.CompanyRoleDTO> companies = user.getCompanyAccesses().stream()
                .sorted(Comparator.comparing(a -> a.getCompany().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(a -> new PlatformUserDTO.CompanyRoleDTO(
                        a.getCompany().getId(), a.getCompany().getName(), a.getRole()))
                .toList();
        return new PlatformUserDTO(user.getId(), user.getUsername(), user.getName(), user.getRole(),
                user.isActive(), user.isPlatformAdmin(), companies);
    }
}
