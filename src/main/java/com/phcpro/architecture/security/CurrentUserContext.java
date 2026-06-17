package com.phcpro.architecture.security;

import com.phcpro.architecture.exception.BusinessRuleException;

public class CurrentUserContext {

    private static final ThreadLocal<UserSession> userSessionThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Long> companyIdThreadLocal = new ThreadLocal<>();

    public record UserSession(String username, String role) {}

    public static void setCurrentUser(String username, String role) {
        userSessionThreadLocal.set(new UserSession(username, role));
    }

    public static UserSession getCurrentUser() {
        UserSession session = userSessionThreadLocal.get();
        if (session == null) {
            // Default fallback for background operations or when headers are omitted
            return new UserSession("SYSTEM", "ADMIN");
        }
        return session;
    }

    public static String getUsername() {
        return getCurrentUser().username();
    }

    public static String getRole() {
        return getCurrentUser().role();
    }

    public static void setCurrentCompanyId(Long companyId) {
        companyIdThreadLocal.set(companyId);
    }

    public static Long getCurrentCompanyId() {
        Long companyId = companyIdThreadLocal.get();
        return companyId == null ? 1L : companyId;
    }

    public static Long requireCurrentCompanyId() {
        Long companyId = companyIdThreadLocal.get();
        if (companyId == null) {
            throw new BusinessRuleException("Selecione uma empresa antes de continuar.");
        }
        return companyId;
    }

    public static void requireCompany(Long companyId) {
        if (companyId == null || !getCurrentCompanyId().equals(companyId)) {
            throw new BusinessRuleException("Operação recusada: o documento pertence a outra empresa.");
        }
    }

    public static void clear() {
        userSessionThreadLocal.remove();
        companyIdThreadLocal.remove();
    }
}
