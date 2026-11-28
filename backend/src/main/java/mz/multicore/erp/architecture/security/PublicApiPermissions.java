package mz.multicore.erp.architecture.security;

import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;

import java.util.List;
import java.util.Optional;

/**
 * Which key permission each public route demands (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.3, API-04).
 * Checked by {@link ApiKeyAuthenticationFilter} before the resource runs; each permission only opens its own routes.
 */
public final class PublicApiPermissions {

    private static final String V1 = PublicApiRoutes.PREFIX + "/v1";

    private record Rule(String method, String prefix, ApiKeyPermission permission) {
        boolean matches(String requestMethod, String path) {
            return method.equalsIgnoreCase(requestMethod) && RequestPaths.isUnder(path, prefix);
        }
    }

    private static final List<Rule> RULES = List.of(
            new Rule("GET", V1 + "/products", ApiKeyPermission.CATALOG_READ),
            new Rule("GET", V1 + "/stock", ApiKeyPermission.CATALOG_READ),
            new Rule("GET", V1 + "/orders", ApiKeyPermission.ORDERS_READ),
            new Rule("POST", V1 + "/orders", ApiKeyPermission.ORDERS_WRITE));

    private PublicApiPermissions() {
    }

    /** @param path request path without the context path; empty when the route demands no specific permission */
    public static Optional<ApiKeyPermission> required(String method, String path) {
        return RULES.stream().filter(rule -> rule.matches(method, path)).map(Rule::permission).findFirst();
    }
}
