package mz.multicore.erp.architecture.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyPrincipal;
import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;
import mz.multicore.erp.modules.publicapi.service.ApiKeyAuthenticationService;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Authentication of the public API (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2, API-03).
 *
 * <p>On {@link PublicApiRoutes#isPublicApi public routes} only {@code X-Api-Key} counts: the key's company is the
 * request's company (no {@code X-Company-Id}) and the {@link ApiKeyPrincipal} becomes the authenticated principal.
 * A user token there, or a missing/unknown/revoked key → 401 with JSON {@code code} + PT-MZ {@code message}.
 * A key over {@link PublicApiRateLimiter its request limit} → 429 {@code RATE_LIMIT_EXCEEDED} with {@code Retry-After}
 * (API-05). A valid key without the {@link PublicApiPermissions route's permission} → 403
 * {@code API_PERMISSION_DENIED} (API-04).
 * On the internal {@code /api/**} an API key is refused (401) — it never stands in for a user token.
 * Not a {@code @Component}, so it is not registered twice: {@link SecurityConfig} creates and wires it.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyAuthenticationService authenticationService;
    private final PublicApiRateLimiter rateLimiter;

    public ApiKeyAuthenticationFilter(ApiKeyAuthenticationService authenticationService,
                                      PublicApiRateLimiter rateLimiter) {
        this.authenticationService = authenticationService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = RequestPaths.withinApplication(request);
        String apiKey = request.getHeader(PublicApiRoutes.API_KEY_HEADER);
        if (!PublicApiRoutes.isPublicApi(path)) {
            if (apiKey != null && path.startsWith("/api/")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "As chaves de API só servem na API pública (" + PublicApiRoutes.PREFIX + ").");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            boolean userToken = request.getHeader("Authorization") != null;
            refuse(response, userToken ? "USER_TOKEN_NOT_ACCEPTED" : "API_KEY_REQUIRED",
                    userToken ? "A API pública não aceita sessões de utilizador. Use o cabeçalho X-Api-Key."
                            : "Indique a chave de API no cabeçalho X-Api-Key.");
            return;
        }
        Optional<ApiKeyPrincipal> principal = authenticationService.authenticate(apiKey);
        if (principal.isEmpty()) {
            refuse(response, "API_KEY_INVALID", "Chave de API inválida ou revogada.");
            return;
        }
        OptionalLong retryAfter = rateLimiter.acquire(principal.get().keyId());
        if (retryAfter.isPresent()) {
            response.setHeader("Retry-After", String.valueOf(retryAfter.getAsLong()));
            refuse(response, 429, "RATE_LIMIT_EXCEEDED", "Limite de " + PublicApiRateLimiter.REQUESTS_PER_MINUTE
                    + " pedidos por minuto atingido para esta chave. Tente novamente em "
                    + retryAfter.getAsLong() + " segundo(s).");
            return;
        }
        Optional<ApiKeyPermission> required = PublicApiPermissions.required(request.getMethod(), path);
        if (required.isPresent() && !principal.get().has(required.get())) {
            refuse(response, HttpServletResponse.SC_FORBIDDEN, "API_PERMISSION_DENIED",
                    "Esta chave de API não tem a permissão " + required.get().name() + " necessária para este pedido.");
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal.get(), null, AuthorityUtils.NO_AUTHORITIES));
        filterChain.doFilter(request, response);
    }

    private static void refuse(HttpServletResponse response, String code, String message) throws IOException {
        refuse(response, HttpServletResponse.SC_UNAUTHORIZED, code, message);
    }

    private static void refuse(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
