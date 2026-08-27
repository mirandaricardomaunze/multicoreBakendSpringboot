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
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthSessionService authSessionService;

    public TokenAuthenticationFilter(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = authorization.substring(7).trim();
                String username = authSessionService.requireValid(token).username();
                var authentication = new UsernamePasswordAuthenticationToken(
                        username, null, AuthorityUtils.NO_AUTHORITIES);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException invalidToken) {
                // Token ausente/expirado/inválido → segue não-autenticado; o authorizeHttpRequests trata.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
