package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.modules.comercial.model.ReservedStock;
import mz.multicore.erp.modules.comercial.service.OrderReservationLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.service.StockLookup;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Free stock (physical stock minus active order reservations) of a company's products in the online shop's pickup
 * stores (docs/LOJA_ONLINE_SPEC.md §3.2–3.3). Shared by the catalogue (LO-03, Disponível/Esgotado) and the checkout
 * (LO-07, choice of the order's store); quantities never leave the online shop services.
 */
final class PickupFreeStock {

    private final Map<String, BigDecimal> free;

    private PickupFreeStock(Map<String, BigDecimal> free) {
        this.free = free;
    }

    static PickupFreeStock load(Long companyId, Collection<Long> pickupWarehouseIds, StockLookup stockLookup,
                                OrderReservationLookup reservationLookup) {
        Set<Long> pickups = Set.copyOf(pickupWarehouseIds);
        Map<String, BigDecimal> free = new HashMap<>();
        for (Stock stock : stockLookup.listForCompany(companyId)) {
            if (stock.getProduct() == null || stock.getWarehouse() == null || stock.getQuantity() == null
                    || !pickups.contains(stock.getWarehouse().getId())) {
                continue;
            }
            free.merge(key(stock.getProduct().getId(), stock.getWarehouse().getId()), stock.getQuantity(),
                    BigDecimal::add);
        }
        for (ReservedStock r : reservationLookup.activeReservationsForCompany(companyId)) {
            String key = key(r.productId(), r.warehouseId());
            if (r.quantity() != null && free.containsKey(key)) {
                free.merge(key, r.quantity().negate(), BigDecimal::add);
            }
        }
        return new PickupFreeStock(free);
    }

    /** Free quantity of the product in the pickup store; zero when there is no stock line. */
    BigDecimal free(Long productId, Long warehouseId) {
        return free.getOrDefault(key(productId, warehouseId), BigDecimal.ZERO);
    }

    /** Whether the product has free stock above zero in at least one pickup store. */
    boolean anyFree(Long productId) {
        String prefix = productId + ":";
        return free.entrySet().stream()
                .anyMatch(e -> e.getKey().startsWith(prefix) && e.getValue().signum() > 0);
    }

    private static String key(Long productId, Long warehouseId) {
        return productId + ":" + warehouseId;
    }
}
