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
            "/api/inventory/transfers", "/api/intelligence", "/api/hr");

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
        return manager;
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
