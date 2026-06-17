package com.phcpro.desktop.session;

import java.time.Instant;
import java.util.List;

public class DesktopSession {

    private final String token;
    private final Instant expiresAt;
    private final String username;
    private final String displayName;
    private final List<CompanyAccess> companies;
    private CompanyAccess activeCompany;

    public DesktopSession(String token, Instant expiresAt, String username, String displayName,
                          List<CompanyAccess> companies) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.username = username;
        this.displayName = displayName;
        this.companies = List.copyOf(companies);
    }

    public void selectCompany(Long companyId) {
        activeCompany = companies.stream()
                .filter(company -> company.id().equals(companyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("A empresa selecionada não pertence ao utilizador."));
    }

    public String token() {
        return token;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }

    public List<CompanyAccess> companies() {
        return companies;
    }

    public CompanyAccess activeCompany() {
        return activeCompany;
    }

    public Long activeCompanyId() {
        return activeCompany == null ? null : activeCompany.id();
    }

    public String activeRole() {
        return activeCompany == null ? null : activeCompany.role();
    }

    public record CompanyAccess(Long id, String name, String role) {}
}
