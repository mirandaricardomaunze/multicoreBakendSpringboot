package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.modules.inventory.model.Warehouse;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Sales warehouses as the public API sees them (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.3.1): active, allowing sales,
 * not transit; named by number or name, case-insensitive. Shared by the stock list and order creation.
 */
final class PublicSalesWarehouses {

    private PublicSalesWarehouses() {
    }

    /** The company's sales warehouses matching {@code wanted}; all of them when {@code wanted} is blank. */
    static List<Warehouse> matching(List<Warehouse> companyWarehouses, String wanted) {
        return companyWarehouses.stream()
                .filter(Warehouse::isSalesWarehouse)
                .filter(byCodeOrName(wanted))
                .toList();
    }

    /** The single sales warehouse named {@code wanted}, or 404 {@code WAREHOUSE_NOT_FOUND}. */
    static Warehouse require(List<Warehouse> companyWarehouses, String wanted) {
        return matching(companyWarehouses, wanted).stream().findFirst()
                .orElseThrow(() -> notFound(wanted));
    }

    static PublicApiNotFoundException notFound(String wanted) {
        return new PublicApiNotFoundException("WAREHOUSE_NOT_FOUND",
                "Armazém de venda " + (wanted == null ? "" : wanted.trim()) + " não encontrado.");
    }

    private static Predicate<Warehouse> byCodeOrName(String warehouse) {
        if (warehouse == null || warehouse.isBlank()) {
            return w -> true;
        }
        String wanted = warehouse.trim().toLowerCase(Locale.ROOT);
        return w -> wanted.equals(lower(w.getWarehouseNumber())) || wanted.equals(lower(w.getName()));
    }

    private static String lower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
