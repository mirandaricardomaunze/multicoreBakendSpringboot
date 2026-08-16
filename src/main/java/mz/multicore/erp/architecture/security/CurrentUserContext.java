package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

import java.util.function.Supplier;

/**
 * Contexto do operador e da empresa activa, propagado por {@link ThreadLocal} e populado pelo
 * {@link SecurityInterceptor} a cada pedido HTTP.
 *
 * <p><b>Fail-closed.</b> Sem contexto não há papel e não há empresa: {@link #getRole()} devolve vazio
 * (o {@link PermissionGuard} recusa) e {@link #getCurrentCompanyId()} lança. Versões anteriores
 * inventavam {@code ("SYSTEM", "ADMIN")} e a empresa {@code 1} — qualquer caminho sem contexto corria
 * com privilégios máximos contra o tenant errado, em silêncio. Quem precisa mesmo de correr sem
 * utilizador (cron, arranque) declara-o em {@link #runAsSystem(Runnable)}.
 *
 * <p>As variantes {@link #findCurrentUser()} e {@link #findCurrentCompanyId()} devolvem {@code null}
 * sem lançar, para infra que legitimamente corre sem tenant — propagação de contexto para threads de
 * fundo e o superadmin, que não tem empresa.
 */
public final class CurrentUserContext {

    /** Etiqueta de auditoria usada quando não há sessão. Não traz papel associado. */
    private static final String SYSTEM_USERNAME = "SYSTEM";

    private static final ThreadLocal<UserSession> userSessionThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Long> companyIdThreadLocal = new ThreadLocal<>();

    public record UserSession(String username, String role) {}

    private CurrentUserContext() {
    }

    public static void setCurrentUser(String username, String role) {
        userSessionThreadLocal.set(new UserSession(username, role));
    }

    /** Sessão activa, ou {@code null} se não houver. Não lança — para infra que propaga contexto. */
    public static UserSession findCurrentUser() {
        return userSessionThreadLocal.get();
    }

    /**
     * Sessão activa. Sem contexto devolve o utilizador {@code SYSTEM} <b>sem papel</b> — o nome é só
     * etiqueta de auditoria; o papel vazio faz o {@link PermissionGuard} recusar.
     */
    public static UserSession getCurrentUser() {
        UserSession session = userSessionThreadLocal.get();
        return session == null ? new UserSession(SYSTEM_USERNAME, "") : session;
    }

    public static String getUsername() {
        return getCurrentUser().username();
    }

    /** Papel do operador, ou vazio se não houver sessão. <b>Nunca</b> devolve um papel privilegiado. */
    public static String getRole() {
        return getCurrentUser().role();
    }

    public static void setCurrentCompanyId(Long companyId) {
        companyIdThreadLocal.set(companyId);
    }

    /** Empresa activa, ou {@code null} se não houver. Não lança — para infra que propaga contexto. */
    public static Long findCurrentCompanyId() {
        return companyIdThreadLocal.get();
    }

    /**
     * Empresa activa. Lança se não houver — nunca assume uma empresa por omissão, senão uma thread sem
     * contexto lê e escreve no tenant errado sem dar erro.
     */
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
     * Corre trabalho automático da aplicação (cron, arranque) com o utilizador {@code SYSTEM} e papel
     * {@code ADMIN}, repondo o contexto anterior no fim — mesmo que a tarefa lance.
     *
     * <p>A elevação é deliberadamente explícita: antes acontecia sozinha em qualquer thread sem
     * contexto. <b>Não usar para servir pedidos HTTP</b> — aí o contexto vem do
     * {@link SecurityInterceptor} e representa um utilizador real.
     */
    public static void runAsSystem(Runnable task) {
        runAsSystem(toSupplier(task));
    }

    /** Versão de {@link #runAsSystem(Runnable)} que devolve valor. */
    public static <T> T runAsSystem(Supplier<T> task) {
        return runAsSystem(null, task);
    }

    /**
     * Como {@link #runAsSystem(Runnable)}, mas ligado a uma <b>empresa explícita</b> — para trabalho
     * automático que escreve num tenant concreto (ex.: povoamento de dados de demonstração). A empresa
     * é um argumento precisamente para não voltar a ser adivinhada.
     */
    public static void runAsSystem(Long companyId, Runnable task) {
        runAsSystem(companyId, toSupplier(task));
    }

    /** Versão de {@link #runAsSystem(Long, Runnable)} que devolve valor. */
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
