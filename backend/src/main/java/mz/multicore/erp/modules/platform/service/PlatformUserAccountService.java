package mz.multicore.erp.modules.platform.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AuthSessionService;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.platform.dto.CreatePlatformUserRequest;
import mz.multicore.erp.modules.platform.dto.PlatformUserDTO;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.model.AppUserCompanyAccess;
import mz.multicore.erp.modules.users.model.UserRole;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class PlatformUserAccountService {

    private final AppUserRepository appUserRepository;
    private final AppUserCompanyAccessRepository companyAccessRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final AuthSessionService authSessionService;

    public PlatformUserAccountService(AppUserRepository appUserRepository,
                                      AppUserCompanyAccessRepository companyAccessRepository,
                                      CompanyRepository companyRepository,
                                      PasswordEncoder passwordEncoder,
                                      AuditLogService auditLogService,
                                      AuthSessionService authSessionService) {
        this.appUserRepository = appUserRepository;
        this.companyAccessRepository = companyAccessRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.authSessionService = authSessionService;
    }

    public List<PlatformUserDTO> listUsers() {
        return appUserRepository.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    public PlatformUserDTO createUser(CreatePlatformUserRequest request) {
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

    public PlatformUserDTO updateUser(String username, String name) {
        AppUser user = requireUser(username);
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome do utilizador é obrigatório.");
        }
        user.setName(name.trim());
        appUserRepository.save(user);
        audit(username, "PLATFORM_USER_UPDATE", "Nome actualizado para '" + user.getName() + "'.");
        return toDto(user);
    }

    public PlatformUserDTO setUserActive(String username, boolean active) {
        AppUser user = requireUser(username);
        if (user.isPlatformAdmin() && !active) {
            throw new BusinessRuleException("Não é possível desactivar o administrador da plataforma.");
        }
        if (!active && user.isActive()) {
            for (AppUserCompanyAccess access : user.getCompanyAccesses()) {
                if ("ADMIN".equalsIgnoreCase(access.getRole())
                        && companyAccessRepository.countActiveByCompanyIdAndRole(
                                access.getCompany().getId(), "ADMIN") <= 1) {
                    throw new BusinessRuleException("Não é possível desactivar o único administrador da empresa "
                            + access.getCompany().getName() + ".");
                }
            }
        }
        user.setActive(active);
        appUserRepository.save(user);
        audit(username, "PLATFORM_USER_STATUS", (active ? "Activado" : "Desactivado") + " o utilizador.");
        return toDto(user);
    }

    public void resetPassword(String username, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 4) {
            throw new BusinessRuleException("A nova senha deve ter pelo menos 4 caracteres.");
        }
        AppUser user = requireUser(username);
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        appUserRepository.save(user);
        if (authSessionService != null) {
            authSessionService.revokeUser(user.getUsername());
        }
        audit(username, "PLATFORM_USER_PASSWORD", "Senha reposta pelo superadmin.");
    }

    public Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
    }

    public AppUser requireUser(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador não encontrado."));
    }

    public void audit(String username, String action, String details) {
        auditLogService.logEvent(CurrentUserContext.getUsername(), null, action,
                "[" + username + "] " + details);
    }

    public PlatformUserDTO toDto(AppUser user) {
        List<PlatformUserDTO.CompanyRoleDTO> companies = user.getCompanyAccesses().stream()
                .sorted(Comparator.comparing(a -> a.getCompany().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(a -> new PlatformUserDTO.CompanyRoleDTO(
                        a.getCompany().getId(), a.getCompany().getName(), a.getRole()))
                .toList();
        return new PlatformUserDTO(user.getId(), user.getUsername(), user.getName(), user.getRole(),
                user.isActive(), user.isPlatformAdmin(), companies);
    }
}
