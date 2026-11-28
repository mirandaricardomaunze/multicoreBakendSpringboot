package mz.multicore.erp.architecture.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Em produção, o portal web do gestor só responde por HTTPS (PORTAL_GESTOR_SPEC §3.5, PG-10).
 *
 * <p>Abrange as páginas ({@code /portal}, {@code /portal/**}) e o endpoint próprio ({@code /api/portal/**}).
 * Conta como HTTPS a ligação directa segura ou o proxy à frente com {@code X-Forwarded-Proto: https};
 * o resto → 403 com mensagem PT-MZ. Fora do perfil {@code prod} não restringe nada (desenvolvimento por http).
 * Não é {@code @Component} para não ser registado duas vezes: o {@link SecurityConfig} cria-o e liga-o.
 */
public class PortalHttpsOnlyFilter extends OncePerRequestFilter {

    static final String HTTPS_REQUIRED_MESSAGE = "O portal só abre por ligação segura (https://).";

    private final boolean enforce;

    PortalHttpsOnlyFilter(boolean enforce) {
        this.enforce = enforce;
    }

    /** Só obriga a HTTPS com o perfil {@code prod} activo. */
    public static PortalHttpsOnlyFilter forEnvironment(Environment environment) {
        return new PortalHttpsOnlyFilter(environment.acceptsProfiles(Profiles.of("prod")));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enforce || !isPortalRoute(RequestPaths.withinApplication(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isHttps(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, HTTPS_REQUIRED_MESSAGE);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isHttps(HttpServletRequest request) {
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    private static boolean isPortalRoute(String path) {
        return RequestPaths.isUnder(path, "/portal") || RequestPaths.isUnder(path, "/api/portal");
    }
}
