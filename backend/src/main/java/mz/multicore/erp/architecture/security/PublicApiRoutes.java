package mz.multicore.erp.architecture.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The versioned public API (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2): {@code /api/public/**} is authenticated only by
 * {@code X-Api-Key} and kept apart from the internal {@code /api/**} of the desktop (tokens + {@code X-Company-Id}).
 */
public final class PublicApiRoutes {

    public static final String PREFIX = "/api/public";
    public static final String PATTERN = PREFIX + "/**";
    public static final String API_KEY_HEADER = "X-Api-Key";

    private PublicApiRoutes() {
    }

    /** @param path request path without the context path */
    public static boolean isPublicApi(String path) {
        return RequestPaths.isUnder(path, PREFIX);
    }

    /** Whether {@code request} targets the public API (its path without the context path). */
    public static boolean isPublicApi(HttpServletRequest request) {
        return isPublicApi(RequestPaths.withinApplication(request));
    }
}
