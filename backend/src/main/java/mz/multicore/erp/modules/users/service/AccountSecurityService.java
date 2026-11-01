package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.architecture.security.AuthSessionService;
import mz.multicore.erp.architecture.security.PasswordPolicy;
import mz.multicore.erp.architecture.security.TotpCodec;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/** O que o titular faz pela sua conta: trocar a senha e configurar a dupla autenticação. */
@Service
public class AccountSecurityService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttachmentCrypto crypto;
    private final AuthSessionService authSessionService;
    private final AuditLogService auditLogService;
    private final boolean twoFactorRequired;
    private final Map<String, String> pendingSecrets = new ConcurrentHashMap<>();

    public AccountSecurityService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                                  AttachmentCrypto crypto, AuthSessionService authSessionService,
                                  AuditLogService auditLogService,
                                  @Value("${security.two-factor.required:false}") boolean twoFactorRequired) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.crypto = crypto;
        this.authSessionService = authSessionService;
        this.auditLogService = auditLogService;
        this.twoFactorRequired = twoFactorRequired;
    }

    /** Administradores e plataforma: são as contas que mais danos podem causar. */
    public boolean mustUseTwoFactor(AppUser user) {
        if (user.getTotpSecret() != null) return true;
        return twoFactorRequired && (user.isPlatformAdmin() || user.getCompanyAccesses().stream()
                .anyMatch(access -> "ADMIN".equalsIgnoreCase(access.getRole())));
    }

    /** Cada código entra uma só vez: um código visto por alguém não abre uma segunda sessão. */
    @Transactional
    public boolean consumeCode(AppUser user, String code) {
        OptionalLong step = TotpCodec.matchingStep(readSecret(user), code, Instant.now());
        return step.isPresent() && appUserRepository.claimTotpStep(user.getId(), step.getAsLong()) == 1;
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = requireUser(username);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessRuleException("A senha actual está incorrecta.");
        }
        PasswordPolicy.validate(newPassword, username);
        if (newPassword.equals(currentPassword)) {
            throw new BusinessRuleException("A nova senha tem de ser diferente da actual.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        appUserRepository.save(user);
        authSessionService.revokeUser(username);
        auditLogService.logEvent(username, null, "USER_PASSWORD_CHANGE", "O titular trocou a senha.");
    }

    public TwoFactorSetup beginTwoFactorSetup(String username) {
        if (requireUser(username).getTotpSecret() != null) {
            throw new BusinessRuleException("A dupla autenticação já está configurada.");
        }
        String secret = TotpCodec.newSecret();
        pendingSecrets.put(username, secret);
        return new TwoFactorSetup(secret, TotpCodec.otpauthUri(username, secret));
    }

    @Transactional
    public void confirmTwoFactorSetup(String username, String code) {
        String secret = pendingSecrets.get(username);
        OptionalLong step = TotpCodec.matchingStep(secret, code, Instant.now());
        if (step.isEmpty()) {
            throw new BusinessRuleException("Código inválido. Confirme a hora do telemóvel e tente novamente.");
        }
        AppUser user = requireUser(username);
        byte[] sealed = crypto.protect(secret.getBytes(StandardCharsets.US_ASCII));
        user.setTotpSecret(Base64.getEncoder().encodeToString(sealed));
        appUserRepository.save(user);
        appUserRepository.claimTotpStep(user.getId(), step.getAsLong());
        pendingSecrets.remove(username);
        authSessionService.revokeUser(username);
        auditLogService.logEvent(username, null, "USER_2FA_ENABLED", "Dupla autenticação configurada.");
    }

    /** Telemóvel perdido: só a plataforma limpa o segredo, e o titular volta a configurar no login. */
    @Transactional
    public void resetTwoFactor(String username) {
        AppUser user = requireUser(username);
        user.setTotpSecret(null);
        appUserRepository.save(user);
        authSessionService.revokeUser(username);
        auditLogService.logCurrent("USER_2FA_RESET", "Dupla autenticação reposta para " + username);
    }

    private String readSecret(AppUser user) {
        byte[] stored = Base64.getDecoder().decode(user.getTotpSecret());
        return new String(crypto.reveal(stored), StandardCharsets.US_ASCII);
    }

    private AppUser requireUser(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Utilizador não encontrado."));
    }

    public record TwoFactorSetup(String secret, String otpauthUri) {}
}
