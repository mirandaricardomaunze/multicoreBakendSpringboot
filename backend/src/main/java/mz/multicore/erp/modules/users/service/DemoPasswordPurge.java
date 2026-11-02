package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.security.AuthSessionService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * Em produção, nenhuma conta antiga fica com senha de demonstração nem em texto simples.
 *
 * <p>Pedir a troca não chega: quem conhece a senha de demonstração também a podia trocar. Por isso a
 * senha é substituída, e a conta só volta a entrar com uma senha reposta pelo administrador.
 */
@Component
@Profile("prod")
@Order(Ordered.LOWEST_PRECEDENCE)
public class DemoPasswordPurge implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoPasswordPurge.class);
    private static final List<String> DEMO_PASSWORDS = List.of("admin", "password", "superadmin");

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final AuditLogService auditLogService;
    private final String superAdminPassword;

    public DemoPasswordPurge(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                             AuthSessionService authSessionService, AuditLogService auditLogService,
                             @Value("${app.superadmin.password:}") String superAdminPassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
        this.auditLogService = auditLogService;
        this.superAdminPassword = superAdminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<AppUser> users = appUserRepository.findAll();
        List<AppUser> demo = users.parallelStream().filter(this::usesDemoPassword).toList();
        boolean noRecovery = superAdminPassword == null || superAdminPassword.isBlank();
        if (noRecovery && demo.stream().anyMatch(AppUser::isPlatformAdmin)) {
            throw new IllegalStateException("Há uma conta de plataforma com senha de demonstração. "
                    + "Defina SUPERADMIN_PASSWORD para a substituir e volte a arrancar.");
        }
        demo.forEach(this::replacePassword);
        users.stream().filter(user -> !demo.contains(user) && isPlainText(user)).forEach(this::hashPassword);
    }

    private boolean usesDemoPassword(AppUser user) {
        String stored = user.getPassword();
        if (stored == null) return false;
        return isPlainText(user)
                ? DEMO_PASSWORDS.contains(stored)
                : DEMO_PASSWORDS.stream().anyMatch(demo -> passwordEncoder.matches(demo, stored));
    }

    private void replacePassword(AppUser user) {
        String replacement = user.isPlatformAdmin() ? superAdminPassword : randomSecret();
        user.setPassword(passwordEncoder.encode(replacement));
        user.setPasswordChangeRequired(true);
        appUserRepository.save(user);
        authSessionService.revokeUser(user.getUsername());
        auditLogService.logEvent(user.getUsername(), null, "USER_DEMO_PASSWORD_REPLACED",
                "Senha de demonstração substituída no arranque.");
        log.warn("Senha de demonstração substituída na conta {}.", user.getUsername());
    }

    private void hashPassword(AppUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        appUserRepository.save(user);
    }

    private static boolean isPlainText(AppUser user) {
        return user.getPassword() != null && !user.getPassword().startsWith("$2");
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
