package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.ManagerPinRateLimiter;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.users.dto.UserSecurityRequestsDTOs.VerifyManagerPinResponse;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PIN de gestor: definição pelo administrador (4 dígitos, BCrypt) e verificação com bloqueio
 * anti-força-bruta por empresa e IP via {@link ManagerPinRateLimiter}.
 */
@Service
public class ManagerPinService {

    private static final String LOCKED_PREFIX = "Demasiadas tentativas de PIN incorreto. Bloqueado por ";
    private static final String LOCKED_SUFFIX = " minuto(s) para protecção de segurança.";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ManagerPinRateLimiter managerPinRateLimiter;
    private final AuditLogService auditLogService;

    public ManagerPinService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                             ManagerPinRateLimiter managerPinRateLimiter, AuditLogService auditLogService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.managerPinRateLimiter = managerPinRateLimiter != null
                ? managerPinRateLimiter
                : new ManagerPinRateLimiter(3, 5);
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void setManagerPin(String username, String pin) {
        UserAdminGuard.requireAdmin();
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        if (pin == null || pin.trim().length() != 4 || !pin.trim().matches("\\d{4}")) {
            throw new BusinessRuleException("O PIN de gestor deve conter exatamente 4 dígitos numéricos.");
        }
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador não encontrado."));
        UserAdminGuard.requireManagedUser(user, companyId);
        user.setManagerPinHash(passwordEncoder.encode(pin.trim()));
        appUserRepository.save(user);
        if (auditLogService != null) {
            auditLogService.logCurrent("USER_PIN_SET",
                    "Definido/atualizado PIN de gestor para utilizador " + username);
        }
    }

    @Transactional(readOnly = true)
    public VerifyManagerPinResponse verifyManagerPin(String pin, String clientIp) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (managerPinRateLimiter.isLocked(companyId, clientIp)) {
            return locked(managerPinRateLimiter.getRemainingLockMinutes(companyId, clientIp));
        }

        if (pin == null || pin.trim().length() != 4) {
            managerPinRateLimiter.recordFailure(companyId, clientIp);
            long remaining = managerPinRateLimiter.getRemainingLockMinutes(companyId, clientIp);
            if (remaining > 0) {
                return locked(remaining);
            }
            return new VerifyManagerPinResponse(false, null, "PIN deve ter 4 dígitos.");
        }

        List<AppUser> users = companyId != null
                ? appUserRepository.findDistinctByCompanyAccessesCompanyIdOrderByName(companyId)
                : appUserRepository.findAll();

        for (AppUser u : users) {
            if (u.isActive() && u.getManagerPinHash() != null) {
                String role = u.getRoleForCompany(companyId);
                if (("ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role))
                        && passwordEncoder.matches(pin.trim(), u.getManagerPinHash())) {
                    managerPinRateLimiter.recordSuccess(companyId, clientIp);
                    return new VerifyManagerPinResponse(true, u.getName(),
                            "PIN de " + u.getName() + " verificado com sucesso.");
                }
            }
        }
        managerPinRateLimiter.recordFailure(companyId, clientIp);
        long remaining = managerPinRateLimiter.getRemainingLockMinutes(companyId, clientIp);
        if (remaining > 0) {
            return locked(remaining);
        }
        return new VerifyManagerPinResponse(false, null, "PIN de gestor incorreto ou sem privilégios.");
    }

    private static VerifyManagerPinResponse locked(long remainingMinutes) {
        return new VerifyManagerPinResponse(false, null, LOCKED_PREFIX + remainingMinutes + LOCKED_SUFFIX);
    }
}
