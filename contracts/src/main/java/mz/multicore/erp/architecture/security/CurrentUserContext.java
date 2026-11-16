package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

import java.util.function.Supplier;

/**
 * Operator and active-company context propagated through {@link ThreadLocal} and populated by
 * {@link SecurityInterceptor} for each HTTP request.
 *
 * <p><b>Fail-closed.</b> Without context there is no role or company: {@link #getRole()} returns empty,
 * causing the backend {@code PermissionGuard} to reject access, and {@link #getCurrentCompanyId()}
 * throws. Earlier versions invented {@code ("SYSTEM", "ADMIN")} and company {@code 1}, silently
 * granting maximum privileges against the wrong tenant. Work that must run without a user, such as
 * scheduled jobs or startup tasks, declares this through {@link #runAsSystem(Runnable)}.
 *
 * <p>{@link #findCurrentUser()} and {@link #findCurrentCompanyId()} return {@code null} without
 * throwing for infrastructure that legitimately runs without a tenant, such as background-thread
 * context propagation and the super administrator, who has no company.
 */
public final class CurrentUserContext {

    /** Audit label used when no session exists. It has no associated role. */
    private static final String SYSTEM_USERNAME = "SYSTEM";

    private static final ThreadLocal<UserSession> userSessionThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Long> companyIdThreadLocal = new ThreadLocal<>();

    public record UserSession(String username, String role) {}

    private CurrentUserContext() {
    }

    public static void setCurrentUser(String username, String role) {
        userSessionThreadLocal.set(new UserSession(username, role));
    }

    /** Active session, or {@code null} when absent. Does not throw, allowing context propagation. */
    public static UserSession findCurrentUser() {
        return userSessionThreadLocal.get();
    }

    /**
     * Returns the active session. Without context, returns user {@code SYSTEM} <b>without a role</b>.
     * The name is only an audit label; the empty role causes the backend {@code PermissionGuard} to reject access.
     */
    public static UserSession getCurrentUser() {
        UserSession session = userSessionThreadLocal.get();
        return session == null ? new UserSession(SYSTEM_USERNAME, "") : session;
    }

    public static String getUsername() {
        return getCurrentUser().username();
    }

    /** Operator role, or empty when no session exists. <b>Never</b> returns a privileged role. */
    public static String getRole() {
        return getCurrentUser().role();
    }

    public static void setCurrentCompanyId(Long companyId) {
        companyIdThreadLocal.set(companyId);
    }

    /** Active company, or {@code null} when absent. Does not throw, allowing context propagation. */
    public static Long findCurrentCompanyId() {
        return companyIdThreadLocal.get();
    }

    /**
     * Returns the active company. Throws when absent and never assumes a default company, preventing
     * a context-free thread from silently reading or writing the wrong tenant.
     */
    public static Long getCompanyId() {
        return getCurrentCompanyId();
    }

    public static Long getCurrentCompanyId() {
        return requireCurrentCompanyId();
    }

    public static Long requireCurrentCompanyId() {
        Long companyId = companyIdThreadLocal.get();
        if (companyId == null) {
            throw new BusinessRuleException("Selecione uma empresa antes de continuar.");
        }
        return companyId;
    }

    public static void requireCompany(Long companyId) {
        if (companyId == null || !requireCurrentCompanyId().equals(companyId)) {
            throw new BusinessRuleException("Operação recusada: o documento pertence a outra empresa.");
        }
    }

    /**
     * Runs automated application work, such as scheduled or startup tasks, as user {@code SYSTEM}
     * with role {@code ADMIN}, restoring the previous context afterward even if the task throws.
     *
     * <p>Elevation is deliberately explicit; it previously occurred automatically on any thread
     * without context. <b>Do not use for HTTP requests</b>, where {@link SecurityInterceptor}
     * supplies context representing a real user.
     */
    public static void runAsSystem(Runnable task) {
        runAsSystem(toSupplier(task));
    }

    /** Value-returning variant of {@link #runAsSystem(Runnable)}. */
    public static <T> T runAsSystem(Supplier<T> task) {
        return runAsSystem(null, task);
    }

    /**
     * Like {@link #runAsSystem(Runnable)}, but bound to an <b>explicit company</b> for automated work
     * that writes to a specific tenant, such as seeding demonstration data. Requiring the company
     * argument prevents it from being inferred.
     */
    public static void runAsSystem(Long companyId, Runnable task) {
        runAsSystem(companyId, toSupplier(task));
    }

    /** Value-returning variant of {@link #runAsSystem(Long, Runnable)}. */
    public static <T> T runAsSystem(Long companyId, Supplier<T> task) {
        UserSession previousUser = userSessionThreadLocal.get();
        Long previousCompanyId = companyIdThreadLocal.get();
        setCurrentUser(SYSTEM_USERNAME, "ADMIN");
        if (companyId != null) {
            setCurrentCompanyId(companyId);
        }
        try {
            return task.get();
        } finally {
            restore(previousUser, previousCompanyId);
        }
    }

    private static Supplier<Void> toSupplier(Runnable task) {
        return () -> {
            task.run();
            return null;
        };
    }

    private static void restore(UserSession user, Long companyId) {
        if (user == null) {
            userSessionThreadLocal.remove();
        } else {
            userSessionThreadLocal.set(user);
        }
        if (companyId == null) {
            companyIdThreadLocal.remove();
        } else {
            companyIdThreadLocal.set(companyId);
        }
    }

    public static void clear() {
        userSessionThreadLocal.remove();
        companyIdThreadLocal.remove();
    }
}
