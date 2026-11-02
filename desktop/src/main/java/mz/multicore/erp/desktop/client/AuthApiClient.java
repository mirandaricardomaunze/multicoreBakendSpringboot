package mz.multicore.erp.desktop.client;

import mz.multicore.erp.desktop.config.DesktopApiConfig;
import mz.multicore.erp.desktop.session.DesktopSession;

import java.time.Instant;
import java.util.List;

public class AuthApiClient {

    private final DesktopApiConfig config;

    public AuthApiClient(DesktopApiConfig config) {
        this.config = config;
    }

    /** O que só a pessoa pode responder durante o login. Devolver null cancela. */
    public interface SecurityPrompts {
        String twoFactorCode();
        String newPassword();
        String setupCode(String secret);
    }

    public DesktopSession login(String username, String password) {
        return login(username, password, null);
    }

    /** Pede o código, a nova senha e a configuração da dupla autenticação, quando o servidor os exigir. */
    public DesktopSession login(String username, String password, SecurityPrompts prompts) {
        String code = null;
        LoginResponse response = send(username, password, null);
        if (response.twoFactorRequired()) {
            code = answered(prompts == null ? null : prompts.twoFactorCode());
            response = send(username, password, code);
        }
        if (response.passwordChangeRequired()) {
            String newPassword = answered(prompts == null ? null : prompts.newPassword());
            new DesktopApiClient(config, restricted(response))
                    .post("/api/auth/change-password", new ChangePasswordRequest(password, newPassword));
            password = newPassword;
            response = send(username, password, code);
        }
        if (response.twoFactorSetupRequired()) {
            DesktopApiClient client = new DesktopApiClient(config, restricted(response));
            TwoFactorSetup setup = client.post("/api/auth/2fa/setup", null, TwoFactorSetup.class);
            code = answered(prompts == null ? null : prompts.setupCode(setup.secret()));
            client.post("/api/auth/2fa/confirm", new TwoFactorCode(code));
            response = send(username, password, code);
        }
        return toSession(response);
    }

    private LoginResponse send(String username, String password, String code) {
        return new DesktopApiClient(config, null)
                .post("/api/auth/login", new LoginRequest(username, password, code), LoginResponse.class);
    }

    private static String answered(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Autenticação cancelada.");
        }
        return value.trim();
    }

    private DesktopSession restricted(LoginResponse response) {
        return toSession(response);
    }

    private DesktopSession toSession(LoginResponse response) {
        return new DesktopSession(
                response.token(), response.expiresAt(), response.username(), response.displayName(),
                response.superAdmin(),
                response.companies().stream()
                        .map(company -> new DesktopSession.CompanyAccess(company.id(), company.name(), company.role()))
                        .toList()
        );
    }

    public void logout(DesktopSession session) {
        new DesktopApiClient(config, session).post("/api/auth/logout", null);
    }

    record LoginRequest(String username, String password, String totpCode) {}

    record ChangePasswordRequest(String currentPassword, String newPassword) {}

    record TwoFactorCode(String code) {}

    record TwoFactorSetup(String secret, String otpauthUri) {}

    record LoginResponse(String token, Instant expiresAt, String username, String displayName, String role,
                         boolean superAdmin, List<CompanyAccess> companies, boolean twoFactorRequired,
                         boolean passwordChangeRequired, boolean twoFactorSetupRequired) {}

    record CompanyAccess(Long id, String name, String role) {}
}
