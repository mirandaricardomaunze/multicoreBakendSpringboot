package mz.multicore.erp.architecture.security;

import java.util.List;

/**
 * The only API routes a portal token opens (PORTAL_GESTOR_SPEC §3.2, PG-05): authentication, the store dashboard,
 * approvals and the portal's own read-only endpoints. Desktop tokens are not limited by this.
 */
final class PortalTokenScope {

    static final String OUT_OF_SCOPE_MESSAGE = "O acesso do portal não abre esta área. Use o programa da loja.";

    private static final List<String> ALLOWED_PREFIXES = List.of("/api/auth", "/api/approvals", "/api/portal");
    private static final String STORE_DASHBOARD = "/api/reports/store-dashboard";

    private PortalTokenScope() {
    }

    /** @param path request path without the context path */
    static boolean allows(String path) {
        if (path == null) {
            return false;
        }
        if (!path.startsWith("/api/")) {
            return true; // the portal page itself, health checks: nothing a token unlocks
        }
        if (path.equals(STORE_DASHBOARD)) {
            return true;
        }
        return ALLOWED_PREFIXES.stream().anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }
}
