package mz.multicore.erp.architecture.security;

/**
 * The public endpoints of the online shop (docs/LOJA_ONLINE_SPEC.md §3.2): {@code /api/shop/{code}/**} without login,
 * read-only catalogue plus placing (and following) orders. No user token, company header or API key is needed.
 */
public final class OnlineShopRoutes {

    public static final String PREFIX = "/api/shop";
    public static final String PATTERN = PREFIX + "/**";

    private OnlineShopRoutes() {
    }

    /** @param path request path without the context path */
    public static boolean isOnlineShop(String path) {
        return RequestPaths.isUnder(path, PREFIX);
    }

    /** {@code POST /api/shop/{code}/orders} — the route held to the hourly order limit. */
    public static boolean isOrderPlacement(String method, String path) {
        if (!"POST".equalsIgnoreCase(method) || !isOnlineShop(path)) {
            return false;
        }
        String[] parts = path.substring(PREFIX.length()).split("/");
        return parts.length == 3 && parts[0].isEmpty() && !parts[1].isEmpty() && "orders".equals(parts[2]);
    }
}
