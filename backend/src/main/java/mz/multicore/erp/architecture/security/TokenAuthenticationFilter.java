package mz.multicore.erp.architecture.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Valida o token opaco (Bearer) contra o {@link AuthSessionService} e, se válido, popula o
 * {@code SecurityContext}. É a fronteira do Spring Security que fecha o filtro (defense-in-depth):
 * o {@link SecurityInterceptor} continua a resolver empresa/papel e a auditar, mas agora o próprio
 * Spring Security recusa {@code /api/**} sem token válido — em vez do antigo {@code permitAll()}.
 *
 * <p>Lenient de propósito: token ausente/ inválido → simplesmente não autentica (deixa o
 * {@code authorizeHttpRequests} devolver 401). Nunca lança, para não curto-circuitar o login.
 * Um token de portal válido fora das rotas do portal ({@link PortalTokenScope}) recebe 403.
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthSessionService authSessionService;

    public TokenAuthenticationFilter(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    /** A API pública só aceita {@code X-Api-Key}: um token de utilizador nunca autentica lá (API-03). */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PublicApiRoutes.isPublicApi(RequestPaths.withinApplication(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            AuthSessionService.AuthSession session;
            try {
                session = authSessionService.requireValid(authorization.substring(7).trim());
            } catch (RuntimeException invalidToken) {
                // Token ausente/expirado/inválido → segue não-autenticado; o authorizeHttpRequests trata.
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            // Token de portal só abre as rotas do portal (PORTAL_GESTOR_SPEC §3.2, PG-05).
            if (session.portal() && !PortalTokenScope.allows(RequestPaths.withinApplication(request))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, PortalTokenScope.OUT_OF_SCOPE_MESSAGE);
                return;
            }
            var authentication = new UsernamePasswordAuthenticationToken(
                    session.username(), null, AuthorityUtils.NO_AUTHORITIES);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
