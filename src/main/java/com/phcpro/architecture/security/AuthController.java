package com.phcpro.architecture.security;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.users.model.AppUser;
import com.phcpro.modules.users.service.AppUserService;
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

    public AuthController(AppUserService appUserService, AuthSessionService authSessionService) {
        this.appUserService = appUserService;
        this.authSessionService = authSessionService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = appUserService.authenticate(request.username(), request.password());
        AuthSessionService.AuthSession session = authSessionService.create(user);

        // Só empresas activas entram na sessão — uma empresa suspensa deixa de ser acessível.
        List<CompanyAccess> companies = user.getCompanyAccesses().stream()
                .filter(access -> access.getCompany().isActive())
                .map(access -> new CompanyAccess(
                        access.getCompany().getId(), access.getCompany().getName(), access.getRole()))
                .toList();

        // Utilizador de tenant sem nenhuma empresa activa não entra; o superadmin pode ter zero.
        if (!user.isPlatformAdmin() && companies.isEmpty()) {
            authSessionService.revoke(session.token());
            throw new BusinessRuleException(
                    "Sem empresa activa. A sua empresa pode estar suspensa — contacte o suporte.");
        }

        return new LoginResponse(
                session.token(), session.expiresAt(), user.getUsername(), user.getName(), user.getRole(),
                user.isPlatformAdmin(), companies
        );
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String authorization) {
        authSessionService.revoke(bearerToken(authorization));
    }

    private String bearerToken(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : null;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record LoginResponse(
            String token,
            Instant expiresAt,
            String username,
            String displayName,
            String role,
            boolean superAdmin,
            List<CompanyAccess> companies
    ) {}

    public record CompanyAccess(Long id, String name, String role) {}
}
