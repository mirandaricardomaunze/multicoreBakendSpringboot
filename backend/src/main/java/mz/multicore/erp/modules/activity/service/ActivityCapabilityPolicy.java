package mz.multicore.erp.modules.activity.service;

import mz.multicore.erp.modules.activity.model.ActivityCapability;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Which HTTP writes start new work that belongs to an activity capability (SA-C01,
 * {@code docs/SECTORES_ACTIVIDADE_SPEC.md}). Only the requests listed here are checked.
 *
 * <p>Deliberately left out: every read, and every write that finishes, settles or undoes existing work
 * (refund or apply an advance, collect paid material, settle a consignment, close a plan, cancel a
 * breakdown, second weighing, approve a measurement). A company that turns a capability off must still be able
 * to wind down what it already has, so no money or stock is trapped.
 */
public final class ActivityCapabilityPolicy {

    private record Rule(String method, String pattern, ActivityCapability capability) { }

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final List<Rule> RULES = List.of(
            new Rule("POST", "/api/advances", ActivityCapability.ADVANCE_PAYMENTS),
            new Rule("PUT", "/api/custom-orders/{orderId}", ActivityCapability.CUSTOM_ORDERS),
            new Rule("POST", "/api/consignments", ActivityCapability.CONSIGNMENT),
            new Rule("PUT", "/api/returnables/items", ActivityCapability.RETURNABLES),
            new Rule("POST", "/api/variants/families", ActivityCapability.VARIANTS),
            new Rule("POST", "/api/variants/families/{id}/values", ActivityCapability.VARIANTS),
            new Rule("POST", "/api/weighbridge/tickets", ActivityCapability.TRUCK_SCALE),
            new Rule("PUT", "/api/butchery/templates", ActivityCapability.CARCASS_BREAKDOWN),
            new Rule("POST", "/api/butchery/breakdowns", ActivityCapability.CARCASS_BREAKDOWN),
            new Rule("PUT", "/api/expiry/rules", ActivityCapability.MARKDOWNS),
            new Rule("POST", "/api/expiry/markdowns", ActivityCapability.MARKDOWNS),
            new Rule("POST", "/api/expiry/markdowns/apply-rules", ActivityCapability.MARKDOWNS),
            new Rule("PUT", "/api/production-plans", ActivityCapability.PRODUCTION),
            new Rule("POST", "/api/production-plans/runs", ActivityCapability.PRODUCTION),
            new Rule("PUT", "/api/projects", ActivityCapability.CUSTOMER_PROJECTS),
            new Rule("PUT", "/api/projects/{id}/orders/{orderId}", ActivityCapability.CUSTOMER_PROJECTS),
            new Rule("PUT", "/api/pricing/lists", ActivityCapability.PRICE_LISTS),
            new Rule("PUT", "/api/pricing/items", ActivityCapability.PRICE_LISTS),
            new Rule("PUT", "/api/pricing/clients", ActivityCapability.PRICE_LISTS),
            new Rule("PUT", "/api/bulk-stock/settings", ActivityCapability.BULK_STOCK),
            new Rule("POST", "/api/bulk-stock/measurements", ActivityCapability.BULK_STOCK),
            new Rule("POST", "/api/bulk-stock/measurements/preview", ActivityCapability.BULK_STOCK),
            new Rule("PUT", "/api/units/products", ActivityCapability.MULTI_UNIT_SALES),
            new Rule("POST", "/api/pickups/accounts", ActivityCapability.PENDING_PICKUP));

    private ActivityCapabilityPolicy() {
    }

    /** The capability a request needs, or empty when the request is not gated. */
    public static Optional<ActivityCapability> required(String method, String path) {
        if (method == null || path == null) return Optional.empty();
        String verb = method.toUpperCase(Locale.ROOT);
        String clean = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
        return RULES.stream()
                .filter(rule -> rule.method().equals(verb) && MATCHER.match(rule.pattern(), clean))
                .map(Rule::capability)
                .findFirst();
    }

    /** Message shown when the company's profile does not include the capability. */
    public static String deniedMessage(ActivityCapability capability) {
        return "«" + capability.label() + "» não está activo no perfil de actividade da empresa. "
                + "Active-o em Configurações › Perfil de Actividade.";
    }
}
