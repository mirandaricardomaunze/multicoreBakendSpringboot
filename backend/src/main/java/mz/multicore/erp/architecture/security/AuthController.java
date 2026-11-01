package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.subscription.service.SubscriptionService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.service.AccountSecurityService;
import mz.multicore.erp.modules.users.service.AppUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserService appUserService;
    private final AuthSessionService authSessionService;
    private final SubscriptionService subscriptionService;
    private final LoginRateLimiter loginRateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final AccountSecurityService accountSecurityService;

    public AuthController(AppUserService appUserService, AuthSessionService authSessionService,
                          SubscriptionService subscriptionService, LoginRateLimiter loginRateLimiter,
                          ClientIpResolver clientIpResolver, AccountSecurityService accountSecurityService) {
        this.accountSecurityService = accountSecurityService;
        this.appUserService = appUserService;
        this.authSessionService = authSessionService;
        this.subscriptionService = subscriptionService;
        this.loginRateLimiter = loginRateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        loginRateLimiter.checkAllowed(request.username(), clientIp);
        AppUser user;
        try {
            user = appUserService.authenticate(request.username(), request.password());
        } catch (RuntimeException ex) {
            loginRateLimiter.recordFailure(request.username(), clientIp); // conta falhas (senha errada, inativo, inexistente)
            throw ex;
        }
        boolean twoFactor = accountSecurityService.mustUseTwoFactor(user);
        if (twoFactor && user.getTotpSecret() != null) {
            if (request.totpCode() == null || request.totpCode().isBlank()) {
                return LoginResponse.twoFactorChallenge(user.getUsername());
            }
            if (!accountSecurityService.consumeCode(user, request.totpCode())) {
                loginRateLimiter.recordFailure(request.username(), clientIp);
                throw new BusinessRuleException(
                        "Código de autenticação inválido ou já usado. Aguarde o próximo código.");
            }
        }
        loginRateLimiter.recordSuccess(request.username(), clientIp);
        boolean setupRequired = twoFactor && user.getTotpSecret() == null;
        AuthSessionService.AuthSession session = authSessionService.create(
                user, user.isPasswordChangeRequired() || setupRequired);

        // Só empresas acessíveis entram na sessão: activas (suspensão manual) e com assinatura que
        // permita login (não expirada/suspensa). Empresa sem assinatura continua acessível.
        List<CompanyAccess> companies = user.getCompanyAccesses().stream()
                .filter(access -> access.getCompany().isActive())
                .filter(access -> subscriptionService.allowsLogin(access.getCompany().getId()))
                .map(access -> new CompanyAccess(
                        access.getCompany().getId(), access.getCompany().getName(), access.getRole()))
                .toList();

        // Utilizador de tenant sem nenhuma empresa acessível não entra; o superadmin pode ter zero.
        if (!user.isPlatformAdmin() && companies.isEmpty()) {
            authSessionService.revoke(session.token());
            throw new BusinessRuleException(
                    "Sem empresa activa. A sua empresa pode estar suspensa ou com a assinatura expirada"
                            + " — contacte o suporte.");
        }

        return new LoginResponse(
                session.token(), session.expiresAt(), user.getUsername(), user.getName(), user.getRole(),
                user.isPlatformAdmin(), companies, false, user.isPasswordChangeRequired(), setupRequired
        );
    }

    @PostMapping("/change-password")
    public void changePassword(@RequestHeader("Authorization") String authorization,
                               @Valid @RequestBody ChangePasswordRequest request) {
        accountSecurityService.changePassword(sessionUser(authorization), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/2fa/setup")
    public AccountSecurityService.TwoFactorSetup beginTwoFactorSetup(@RequestHeader("Authorization") String authorization) {
        return accountSecurityService.beginTwoFactorSetup(sessionUser(authorization));
    }

    @PostMapping("/2fa/confirm")
    public void confirmTwoFactorSetup(@RequestHeader("Authorization") String authorization,
                                      @Valid @RequestBody TwoFactorCodeRequest request) {
        accountSecurityService.confirmTwoFactorSetup(sessionUser(authorization), request.code());
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String authorization) {
        authSessionService.revoke(bearerToken(authorization));
    }

    private String sessionUser(String authorization) {
        return authSessionService.requireValid(bearerToken(authorization)).username();
    }

    private String bearerToken(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : null;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password, String totpCode) {
        public LoginRequest(String username, String password) {
            this(username, password, null);
        }
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}

    public record TwoFactorCodeRequest(@NotBlank String code) {}

    public record LoginResponse(
            String token,
            Instant expiresAt,
            String username,
            String displayName,
            String role,
            boolean superAdmin,
            List<CompanyAccess> companies,
            boolean twoFactorRequired,
            boolean passwordChangeRequired,
            boolean twoFactorSetupRequired
    ) {
        static LoginResponse twoFactorChallenge(String username) {
            return new LoginResponse(null, null, username, null, null, false, List.of(), true, false, false);
        }
    }

    public record CompanyAccess(Long id, String name, String role) {}
}
