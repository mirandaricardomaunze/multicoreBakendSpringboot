package com.phcpro.architecture.security;

public class CurrentUserContext {

    private static final ThreadLocal<UserSession> userSessionThreadLocal = new ThreadLocal<>();
    private static Long currentCompanyId = 1L; // Default fallback to first company

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
        currentCompanyId = companyId;
    }

    public static Long getCurrentCompanyId() {
        return currentCompanyId;
    }

    public static void clear() {
        userSessionThreadLocal.remove();
    }
}
