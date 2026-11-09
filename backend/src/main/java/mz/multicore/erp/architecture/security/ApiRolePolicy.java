package mz.multicore.erp.architecture.security;

import java.util.Locale;
import java.util.Set;

/** Permissões HTTP por área; os Services mantêm as guardas das operações sensíveis. */
public final class ApiRolePolicy {

    private static final Set<String> MANAGER_AREAS = Set.of(
            "/api/finance", "/api/accounting", "/api/fiscal", "/api/audit",
            "/api/reports", "/api/purchases", "/api/business-units",
            "/api/approvals", "/api/assets", "/api/manufacturing",
            "/api/fleet", "/api/performance", "/api/movimentos",
            "/api/comercial/receivables", "/api/comercial/credit-risk",
            "/api/comercial/statements", "/api/comercial/receipts",
            "/api/comercial/delivery-guides", "/api/credit-notes",
            "/api/debit-notes", "/api/recurring-contracts",
            "/api/recurring/contracts", "/api/inventory/counts",
            "/api/inventory/physical-counting", "/api/inventory/waste",
            "/api/inventory/transfers", "/api/inventory/movements",
            "/api/inventory/warehouses/all", "/api/intelligence", "/api/hr", "/api/quality");

    /** Raízes que têm uma decisão deliberada nesta política, mesmo quando a decisão fina é por método. */
    private static final Set<String> EXPLICIT_AREAS = Set.of(
            "/api/auth", "/api/version", "/api/platform", "/api/monitoring",
            "/api/backup", "/api/users", "/api/documents",
            "/api/license", "/api/subscription", "/api/dashboard", "/api/crm",
            "/api/support/tickets", "/api/promotions", "/api/comercial", "/api/pos",
            "/api/product-categories", "/api/inventory", "/api/print",
            "/api/finance", "/api/accounting", "/api/fiscal", "/api/audit",
            "/api/reports", "/api/purchases", "/api/business-units", "/api/approvals",
            "/api/assets", "/api/manufacturing", "/api/fleet", "/api/performance",
            "/api/movimentos", "/api/credit-notes", "/api/debit-notes",
            "/api/recurring-contracts", "/api/recurring/contracts", "/api/intelligence",
            "/api/hr", "/api/quality", "/api/activity", "/api/units", "/api/pricing", "/api/pickups", "/api/bulk-stock", "/api/projects",
            "/api/advances", "/api/butchery", "/api/expiry", "/api/production-plans",
            "/api/custom-orders", "/api/consignments",
            "/api/returnables", "/api/variants", "/api/weighbridge", "/api/exceptions");

    /** Business-sector areas (SA-C01..C03): every role may read, only management may change. */
    private static final Set<String> READ_SELLER_WRITE_MANAGER_AREAS = Set.of(
            "/api/activity", "/api/units", "/api/pricing");

    private ApiRolePolicy() {
    }

    public static boolean allows(String role, String method, String uri) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if ("ADMIN".equals(normalized)) {
            return true;
        }
        if (uri == null || method == null) {
            return false;
        }
        if (matches(uri, "/api/backup") || (matches(uri, "/api/users")
                && !"/api/users/verify-pin".equals(uri))) {
            return false;
        }
        if ("GET".equalsIgnoreCase(method)
                && uri.matches("/api/hr/occupational-health/employee/[^/]+/summary")) {
            return !normalized.isBlank();
        }
        boolean manager = "MANAGER".equals(normalized);
        if (MANAGER_AREAS.stream().anyMatch(area -> matches(uri, area))) {
            return manager;
        }
        boolean seller = "SELLER".equals(normalized) || manager;
        if (matches(uri, "/api/license") || matches(uri, "/api/subscription")) {
            return !normalized.isBlank();
        }
        if (matches(uri, "/api/dashboard")) {
            return seller;
        }
        if (matches(uri, "/api/crm")) {
            return seller;
        }
        if (matches(uri, "/api/support/tickets")) {
            return seller;
        }
        if (READ_SELLER_WRITE_MANAGER_AREAS.stream().anyMatch(area -> matches(uri, area))) {
            return "GET".equalsIgnoreCase(method) ? seller : manager;
        }
        if (matches(uri, "/api/exceptions")) {
            return manager;
        }
        if (matches(uri, "/api/weighbridge")) {
            return seller;
        }
        if (matches(uri, "/api/variants")) {
            return "GET".equalsIgnoreCase(method) ? seller : manager;
        }
        if (matches(uri, "/api/returnables")) {
            boolean managerOnly = "PUT".equalsIgnoreCase(method) || uri.endsWith("/charges");
            return managerOnly ? manager : seller;
        }
        if (matches(uri, "/api/consignments")) {
            return uri.endsWith("/settle") ? manager : seller;
        }
        if (matches(uri, "/api/custom-orders")) {
            return seller;
        }
        if (matches(uri, "/api/production-plans")) {
            return seller;
        }
        if (matches(uri, "/api/expiry")) {
            boolean managerOnly = "PUT".equalsIgnoreCase(method) || uri.contains("/markdowns");
            return managerOnly ? manager : seller;
        }
        if (matches(uri, "/api/butchery")) {
            boolean managerOnly = "PUT".equalsIgnoreCase(method) || uri.endsWith("/cancel");
            return managerOnly ? manager : seller;
        }
        if (matches(uri, "/api/advances")) {
            return "GET".equalsIgnoreCase(method) ? seller : manager;
        }
        if (matches(uri, "/api/projects")) {
            return "DELETE".equalsIgnoreCase(method) ? manager : seller;
        }
        if (matches(uri, "/api/bulk-stock")) {
            boolean managerOnly = ("PUT".equalsIgnoreCase(method) && uri.endsWith("/settings"))
                    || uri.endsWith("/approve") || uri.endsWith("/reject");
            return managerOnly ? manager : seller;
        }
        if (matches(uri, "/api/pickups")) {
            return uri.endsWith("/cancel") ? manager : seller;
        }
        if (matches(uri, "/api/promotions")) {
            return "GET".equalsIgnoreCase(method) ? seller : manager;
        }
        if (matches(uri, "/api/comercial/quotations")) {
            return !uri.endsWith("/convert-to-invoice") ? seller : manager;
        }
        if (matches(uri, "/api/pos")) {
            return seller && (!matches(uri, "/api/pos/returns") || manager);
        }
        if (matches(uri, "/api/comercial/orders")) {
            if (uri.matches(".*/(approve|reject|cancel|bill|transfer)$")) {
                return manager;
            }
            return seller;
        }
        if (matches(uri, "/api/comercial/invoices")) {
            return "GET".equalsIgnoreCase(method) ? seller : manager;
        }
        if (matches(uri, "/api/comercial/products") || matches(uri, "/api/product-categories")) {
            return "GET".equalsIgnoreCase(method) ? seller : manager;
        }
        if (matches(uri, "/api/comercial/clients")) {
            return "DELETE".equalsIgnoreCase(method) ? manager : seller;
        }
        if (matches(uri, "/api/inventory")) {
            return "GET".equalsIgnoreCase(method) ? seller : manager;
        }
        if (matches(uri, "/api/print")) {
            return allowsPrint(seller, manager, uri);
        }
        return manager && isExplicitlyClassified(method, uri);
    }

    /**
     * Indica se um mapping HTTP foi conscientemente incluído na matriz. É separado de
     * {@link #allows(String, String, String)} para o harness poder detectar endpoints novos sem
     * confundir "negado" com "esquecido".
     */
    public static boolean isExplicitlyClassified(String method, String uri) {
        if (method == null || method.isBlank() || uri == null || uri.isBlank()) {
            return false;
        }
        return EXPLICIT_AREAS.stream().anyMatch(area -> matches(uri, area));
    }

    private static boolean allowsPrint(boolean seller, boolean manager, String uri) {
        if (manager) {
            return true;
        }
        if (!seller) {
            return false;
        }
        return matches(uri, "/api/print/invoice")
                || matches(uri, "/api/print/order")
                || matches(uri, "/api/print/quotation")
                || matches(uri, "/api/print/receipt")
                || matches(uri, "/api/print/pos-z-report")
                || matches(uri, "/api/print/pos-voucher")
                || matches(uri, "/api/print/product-label")
                || matches(uri, "/api/print/status");
    }

    private static boolean matches(String uri, String area) {
        return uri.equals(area) || uri.startsWith(area + "/");
    }
}
