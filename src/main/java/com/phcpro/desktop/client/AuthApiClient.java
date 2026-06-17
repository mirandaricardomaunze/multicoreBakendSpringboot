package com.phcpro.desktop.client;

import com.phcpro.desktop.config.DesktopApiConfig;
import com.phcpro.desktop.session.DesktopSession;

import java.time.Instant;
import java.util.List;

public class AuthApiClient {

    private final DesktopApiConfig config;

    public AuthApiClient(DesktopApiConfig config) {
        this.config = config;
    }

    public DesktopSession login(String username, String password) {
        LoginResponse response = new DesktopApiClient(config, null)
                .post("/api/auth/login", new LoginRequest(username, password), LoginResponse.class);
        return new DesktopSession(
                response.token(), response.expiresAt(), response.username(), response.displayName(),
                response.companies().stream()
                        .map(company -> new DesktopSession.CompanyAccess(company.id(), company.name(), company.role()))
                        .toList()
        );
    }

    public void logout(DesktopSession session) {
        new DesktopApiClient(config, session).post("/api/auth/logout", null);
    }

    record LoginRequest(String username, String password) {}

    record LoginResponse(String token, Instant expiresAt, String username, String displayName, String role,
                         List<CompanyAccess> companies) {}

    record CompanyAccess(Long id, String name, String role) {}
}
