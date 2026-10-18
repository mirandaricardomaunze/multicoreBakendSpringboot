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
        if (uri != null && uri.startsWith("/api/monitoring/")) {
            return handleMonitoring(request, response, authorization);
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
            String role = user.getRoleForCompany(requestedCompanyId);
            if (!ApiRolePolicy.allows(role, request.getMethod(), uri)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Sem permissão para aceder a esta área com o perfil actual.");
                return false;
            }
            CurrentUserContext.setCurrentUser(user.getUsername(), role);
            CurrentUserContext.setCurrentCompanyId(requestedCompanyId);
            updateSecurityContextAuthority(user.getUsername(), role);
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
            updateSecurityContextAuthority(user.getUsername(), PermissionGuard.SUPERADMIN_ROLE);
            return true;
        } catch (RuntimeException ex) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
            return false;
        }
    }

    private boolean handleMonitoring(HttpServletRequest request, HttpServletResponse response, String authorization)
            throws Exception {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token é obrigatório.");
            return false;
        }
        try {
            String token = authorization.substring(7).trim();
            String username = authSessionService.requireValid(token).username();
            String companyHeader = request.getHeader("X-Company-Id");
            if (companyHeader == null || companyHeader.isBlank()) {
                var user = tenantAccessService.requireSuperAdmin(username);
                CurrentUserContext.setCurrentUser(user.getUsername(), PermissionGuard.SUPERADMIN_ROLE);
            } else {
                Long companyId = Long.parseLong(companyHeader);
                var user = tenantAccessService.requireAccess(username, companyId);
                String role = user.getRoleForCompany(companyId);
                if (!"ADMIN".equalsIgnoreCase(role)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "A monitorização exige perfil ADMIN.");
                    return false;
                }
                String uri = request.getRequestURI();
                if (uri != null && (uri.endsWith("/incidents") || uri.endsWith("/test-email-alert"))) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Esta operação exige administrador da plataforma.");
                    return false;
                }
                CurrentUserContext.setCurrentUser(user.getUsername(), role);
                CurrentUserContext.setCurrentCompanyId(companyId);
                updateSecurityContextAuthority(user.getUsername(), role);
            }
            return true;
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empresa inválida.");
            return false;
        } catch (RuntimeException ex) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private void updateSecurityContextAuthority(String username, String role) {
        if (role == null || role.isBlank()) {
            return;
        }
        String authority = role.startsWith("ROLE_")
                ? role.toUpperCase(java.util.Locale.ROOT)
                : "ROLE_" + role.toUpperCase(java.util.Locale.ROOT);
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                username, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority)));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
