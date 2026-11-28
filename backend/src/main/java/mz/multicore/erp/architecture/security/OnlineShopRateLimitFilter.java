package mz.multicore.erp.architecture.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.OptionalLong;

/**
 * Per-IP limits of the public online shop (docs/LOJA_ONLINE_SPEC.md §3.2, LO-04): over
 * {@value OnlineShopRateLimiter#REQUESTS_PER_MINUTE} requests a minute, or {@value OnlineShopRateLimiter#ORDERS_PER_HOUR}
 * order attempts an hour, the shop answers 429 {@code RATE_LIMIT_EXCEEDED} with {@code Retry-After} before any
 * controller runs. The client IP comes from {@link ClientIpResolver} (X-Forwarded-For only from the trusted proxy).
 * Not a {@code @Component}, so it is not registered twice: {@link SecurityConfig} creates and wires it.
 */
public class OnlineShopRateLimitFilter extends OncePerRequestFilter {

    private final OnlineShopRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public OnlineShopRateLimitFilter(OnlineShopRateLimiter rateLimiter, ClientIpResolver clientIpResolver) {
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !OnlineShopRoutes.isOnlineShop(RequestPaths.withinApplication(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = clientIpResolver.resolve(request);
        OptionalLong retryAfter = rateLimiter.acquireRequest(clientIp);
        if (retryAfter.isPresent()) {
            JsonErrorResponses.tooManyRequests(response, retryAfter.getAsLong(), "Demasiados pedidos à loja. "
                    + "Tente novamente em " + retryAfter.getAsLong() + " segundo(s).");
            return;
        }
        if (OnlineShopRoutes.isOrderPlacement(request.getMethod(), RequestPaths.withinApplication(request))) {
            retryAfter = rateLimiter.acquireOrder(clientIp);
            if (retryAfter.isPresent()) {
                JsonErrorResponses.tooManyRequests(response, retryAfter.getAsLong(), "Limite de "
                        + OnlineShopRateLimiter.ORDERS_PER_HOUR + " encomendas por hora atingido. "
                        + "Tente novamente mais tarde ou contacte a loja.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
