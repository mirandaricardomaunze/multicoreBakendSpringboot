package mz.multicore.erp.modules.comercial.service;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.ReservedStock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.StockAvailabilityTracker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JPA listener on {@link Order} and {@link OrderLine} (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1, WH-02): remembers,
 * per writing transaction, what every order and line it reads or creates held reserved at the start, so the inventory
 * can tell whether a reservation-only change (an order reserving or freeing the last units) made the available stock
 * cross zero, and use the reservation at the start of the transaction in the "before". One listener instead of one
 * call per {@code setReservedQuantity}/{@code setReservationActive}: reserving, editing, picking, billing, cancelling
 * and future paths are all covered.
 *
 * <p>The per-transaction state is that transaction's synchronization, found by type: Hibernate may build its own
 * instance of this listener and still share the state with the bean.
 */
@Component
public class OrderReservationWatcher {

    private final ObjectProvider<StockAvailabilityTracker> stockTracker;

    public OrderReservationWatcher(ObjectProvider<StockAvailabilityTracker> stockTracker) {
        this.stockTracker = stockTracker;
    }

    @PostLoad
    public void loaded(Object entity) {
        Watch watch = currentWatch();
        if (watch == null) {
            return;
        }
        if (entity instanceof Order order) {
            watch.orders().putIfAbsent(order.getId(), new WatchedOrder(order, reachOf(order)));
        } else if (entity instanceof OrderLine line) {
            watch.lines().putIfAbsent(line, new LineStart(productIdOf(line), quantityOf(line.getReservedQuantity())));
        }
    }

    @PostPersist
    public void created(Object entity) {
        Watch watch = currentWatch();
        if (watch == null) {
            return;
        }
        // Nothing was reserved before the row existed.
        if (entity instanceof Order order) {
            watch.orders().putIfAbsent(order.getId(), new WatchedOrder(order, Reach.NONE));
        } else if (entity instanceof OrderLine line) {
            watch.lines().putIfAbsent(line, new LineStart(productIdOf(line), BigDecimal.ZERO));
        }
    }

    @PostRemove
    public void removed(Object entity) {
        Watch watch = currentWatch();
        if (watch == null) {
            return;
        }
        if (entity instanceof Order order) {
            watch.removedOrders().add(order.getId());
        } else if (entity instanceof OrderLine line) {
            watch.removedLines().put(line, Boolean.TRUE);
        }
    }

    /** True when the current transaction read or created an order or line. */
    boolean isWatching() {
        return findWatch() != null;
    }

    /**
     * Net change of active reservations made by the current transaction, per product × warehouse (after − start, only
     * non-zero). Expects the persistence context already flushed, so removed lines are known.
     */
    List<ReservedStock> changesOfCurrentTransaction() {
        Watch watch = findWatch();
        if (watch == null) {
            return List.of();
        }
        // An order whose reach changed moves all its lines, even the ones this transaction never read.
        for (WatchedOrder watched : new ArrayList<>(watch.orders().values())) {
            if (!watched.start().equals(currentReach(watch, watched.order()))) {
                watched.order().getLines().forEach(line -> loaded(line));
            }
        }
        Map<StockKey, BigDecimal> deltas = new LinkedHashMap<>();
        for (Map.Entry<OrderLine, LineStart> entry : new ArrayList<>(watch.lines().entrySet())) {
            OrderLine line = entry.getKey();
            LineStart start = entry.getValue();
            Order order = line.getOrder();
            WatchedOrder watchedOrder = order == null ? null : watch.orders().get(order.getId());
            Reach startReach = watchedOrder != null ? watchedOrder.start() : currentReach(watch, order);
            if (startReach.reserves()) {
                deltas.merge(new StockKey(start.productId(), startReach.warehouseId()), start.reserved().negate(),
                        BigDecimal::add);
            }
            Reach nowReach = watch.removedLines().containsKey(line) ? Reach.NONE : currentReach(watch, order);
            if (nowReach.reserves()) {
                deltas.merge(new StockKey(productIdOf(line), nowReach.warehouseId()),
                        quantityOf(line.getReservedQuantity()), BigDecimal::add);
            }
        }
        List<ReservedStock> changes = new ArrayList<>();
        deltas.forEach((key, delta) -> {
            if (delta.signum() != 0 && key.productId() != null) {
                changes.add(new ReservedStock(key.productId(), key.warehouseId(), delta));
            }
        });
        return changes;
    }

    private static Reach currentReach(Watch watch, Order order) {
        return order == null || watch.removedOrders().contains(order.getId()) ? Reach.NONE : reachOf(order);
    }

    private static Reach reachOf(Order order) {
        Warehouse warehouse = order.getWarehouse();
        return order.isReservationActive() && warehouse != null ? new Reach(true, warehouse.getId()) : Reach.NONE;
    }

    private static Long productIdOf(OrderLine line) {
        return line.getProduct() == null ? null : line.getProduct().getId();
    }

    private static BigDecimal quantityOf(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** This writing transaction's watch, created on first use; null outside a writing transaction. */
    private Watch currentWatch() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return null;
        }
        Watch watch = findWatch();
        if (watch == null) {
            watch = new Watch(new HashMap<>(), new IdentityHashMap<>(), new HashSet<>(), new IdentityHashMap<>());
            TransactionSynchronizationManager.registerSynchronization(watch);
            // Reservation-only changes must reach the inventory's commit check even when no stock row is touched.
            stockTracker.ifAvailable(StockAvailabilityTracker::watchCurrentTransaction);
        }
        return watch;
    }

    private static Watch findWatch() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return null;
        }
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            if (synchronization instanceof Watch watch) {
                return watch;
            }
        }
        return null;
    }

    /** Where an order's reservation counts: active and with a warehouse, or nowhere. */
    private record Reach(boolean reserves, Long warehouseId) {
        static final Reach NONE = new Reach(false, null);
    }

    private record WatchedOrder(Order order, Reach start) {
    }

    private record LineStart(Long productId, BigDecimal reserved) {
    }

    private record StockKey(Long productId, Long warehouseId) {
    }

    /** What one transaction read or created, kept as its synchronization so REQUIRES_NEW gets its own. */
    private record Watch(Map<Long, WatchedOrder> orders, Map<OrderLine, LineStart> lines, Set<Long> removedOrders,
                         Map<OrderLine, Boolean> removedLines) implements TransactionSynchronization {
    }
}
