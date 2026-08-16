package mz.multicore.erp.architecture.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    private final TenantAccessService tenantAccessService;
    private final AuthSessionService authSessionService;

    public SecurityInterceptor(TenantAccessService tenantAccessService, AuthSessionService authSessionService) {
        this.tenantAccessService = tenantAccessService;
        this.authSessionService = authSessionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");

        // Endpoints de plataforma são geridos pelo superadmin: token obrigatório, mas sem empresa.
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/platform/")) {
            return handlePlatform(request, response, authorization);
        }

        String companyId = request.getHeader("X-Company-Id");
        if (authorization == null || !authorization.startsWith("Bearer ")
                || companyId == null || companyId.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token e empresa são obrigatórios.");
            return false;
        }

        try {
            String token = authorization.substring(7).trim();
            String username = authSessionService.requireValid(token).username();
            Long requestedCompanyId = Long.parseLong(companyId);
            var user = tenantAccessService.requireAccess(username, requestedCompanyId);
            CurrentUserContext.setCurrentUser(user.getUsername(), user.getRoleForCompany(requestedCompanyId));
            CurrentUserContext.setCurrentCompanyId(requestedCompanyId);
            return true;
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empresa inválida.");
        } catch (RuntimeException ex) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
        }
        return false;
    }

    private boolean handlePlatform(HttpServletRequest request, HttpServletResponse response, String authorization)
            throws Exception {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token é obrigatório.");
            return false;
        }
        try {
            String token = authorization.substring(7).trim();
            String username = authSessionService.requireValid(token).username();
            var user = tenantAccessService.requireSuperAdmin(username);
            CurrentUserContext.setCurrentUser(user.getUsername(), PermissionGuard.SUPERADMIN_ROLE);
            return true;
        } catch (RuntimeException ex) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }
}
