package mz.multicore.erp.architecture.changes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Locale;
import java.util.Set;

/**
 * Bumps the company change counter after every successful write.
 *
 * <p>Ordered after the {@code SecurityInterceptor} (order 0): its {@code afterCompletion} runs
 * first in reverse order, so the active company is still set when this one reads it.
 */
@Configuration
public class ChangeTrackingInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    static final int ORDER = 200;
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final ChangeVersionRegistry registry;

    public ChangeTrackingInterceptor(ChangeVersionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void addInterceptors(InterceptorRegistry interceptors) {
        interceptors.addInterceptor(this)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/changes/**", "/api/changes", "/api/version")
                .order(ORDER);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null || !isWrite(request.getMethod())) return;
        int status = response.getStatus();
        if (status < 200 || status >= 300) return;
        registry.bump(CurrentUserContext.findCurrentCompanyId());
    }

    private static boolean isWrite(String method) {
        return method != null && WRITE_METHODS.contains(method.toUpperCase(Locale.ROOT));
    }
}
