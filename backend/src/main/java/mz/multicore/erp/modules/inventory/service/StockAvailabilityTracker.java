package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.events.StockAvailabilityChangedEvent;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.OrderReservationLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Publishes {@link StockAvailabilityChangedEvent} when the available stock of a product in a sales warehouse crosses
 * zero within a transaction (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1, WH-02). Each writing transaction keeps the
 * stock rows it touched; just before the commit it compares, per product × warehouse, the quantity at the start of the
 * transaction with the one being committed — several movements of the same row in one transaction count once (first
 * before × last after). Available = quantity − active order reservations, never negative (the {@code GET /stock}
 * calculation, §3.3.1). Only sales warehouses and stock-tracked products; changes that do not cross zero publish
 * nothing. The event is published inside the transaction, so after-commit listeners (WH-03) only see committed facts.
 */
@Component
public class StockAvailabilityTracker {

    private final ApplicationEventPublisher eventPublisher;
    private final OrderReservationLookup reservationLookup;

    public StockAvailabilityTracker(ApplicationEventPublisher eventPublisher,
                                    OrderReservationLookup reservationLookup) {
        this.eventPublisher = eventPublisher;
        this.reservationLookup = reservationLookup;
    }

    /**
     * Starts watching {@code stock} in the current writing transaction, remembering {@code quantityAtStart} the first
     * time; outside a transaction or in a read-only one nothing can change, so nothing is watched.
     */
    void watch(Stock stock, BigDecimal quantityAtStart) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return;
        }
        if (currentWatch().add(stock)) {
            stock.rememberQuantityAtStart(quantityAtStart);
        }
    }

    /** Availability change of one watched row, or empty when it did not cross zero or is out of scope. */
    Optional<StockAvailabilityChangedEvent> crossingOf(Stock stock) {
        BigDecimal before = stock.quantityAtStart();
        BigDecimal after = stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity();
        Product product = stock.getProduct();
        Warehouse warehouse = stock.getWarehouse();
        if (before.compareTo(after) == 0 || product == null || warehouse == null
                || !product.isStockTracked() || !warehouse.isSalesWarehouse()) {
            return Optional.empty();
        }
        BigDecimal reserved = reservationLookup.activeReservation(product.getId(), warehouse.getId());
        StockAvailabilityChangedEvent event = new StockAvailabilityChangedEvent(
                warehouse.getCompany() == null ? null : warehouse.getCompany().getId(),
                product.getSku(), product.getName(), warehouse.getWarehouseNumber(), warehouse.getName(),
                available(before, reserved), available(after, reserved), OffsetDateTime.now());
        return event.wentOutOfStock() || event.cameBackInStock() ? Optional.of(event) : Optional.empty();
    }

    private static BigDecimal available(BigDecimal quantity, BigDecimal reserved) {
        return quantity.subtract(reserved).max(BigDecimal.ZERO);
    }

    private Set<Stock> currentWatch() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            if (synchronization instanceof Watch watch && watch.owner() == this) {
                return watch.stocks();
            }
        }
        Watch watch = new Watch(this, Collections.newSetFromMap(new IdentityHashMap<>()));
        TransactionSynchronizationManager.registerSynchronization(watch);
        return watch.stocks();
    }

    /**
     * The rows watched by one transaction. Kept as that transaction's synchronization (not a bound resource) so a
     * nested {@code REQUIRES_NEW} transaction gets its own watch.
     */
    private record Watch(StockAvailabilityTracker owner, Set<Stock> stocks) implements TransactionSynchronization {
        @Override
        public void beforeCommit(boolean readOnly) {
            if (readOnly) {
                return;
            }
            List<Stock> touched = new ArrayList<>(stocks);
            touched.forEach(stock -> owner.crossingOf(stock).ifPresent(owner.eventPublisher::publishEvent));
        }
    }
}
