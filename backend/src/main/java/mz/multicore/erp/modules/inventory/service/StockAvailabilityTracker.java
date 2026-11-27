package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.events.StockAvailabilityChangedEvent;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ReservedStock;
import mz.multicore.erp.modules.comercial.service.OrderReservationLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Publishes {@link StockAvailabilityChangedEvent} when the available stock of a product in a sales warehouse crosses
 * zero within a transaction (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1, WH-02). Each writing transaction keeps the
 * stock rows it touched; just before the commit it compares, per product × warehouse, the quantity at the start of the
 * transaction with the one being committed — several movements of the same row in one transaction count once (first
 * before × last after). Available = quantity − active order reservations, never negative (the {@code GET /stock}
 * calculation, §3.3.1); the "before" uses the reservation at the start of the transaction, and reservation-only
 * changes (an order reserving or freeing the last units, told by {@link OrderReservationLookup}) count too, so billing
 * the last reserved units is not {@code OUT_OF_STOCK}. Only sales warehouses and stock-tracked products; changes that
 * do not cross zero publish nothing. The event is published inside the transaction, so after-commit listeners (WH-03)
 * only see committed facts.
 */
@Component
public class StockAvailabilityTracker {

    private final ApplicationEventPublisher eventPublisher;
    private final OrderReservationLookup reservationLookup;
    private final StockRepository stockRepository;

    public StockAvailabilityTracker(ApplicationEventPublisher eventPublisher,
                                    OrderReservationLookup reservationLookup,
                                    StockRepository stockRepository) {
        this.eventPublisher = eventPublisher;
        this.reservationLookup = reservationLookup;
        this.stockRepository = stockRepository;
    }

    /**
     * Makes sure the current writing transaction is checked before its commit even when it touches no stock row
     * (reservation-only changes); nothing outside a writing transaction.
     */
    public void watchCurrentTransaction() {
        if (isWritingTransaction()) {
            currentWatch();
        }
    }

    /**
     * Starts watching {@code stock} in the current writing transaction, remembering {@code quantityAtStart} the first
     * time; outside a transaction or in a read-only one nothing can change, so nothing is watched.
     */
    void watch(Stock stock, BigDecimal quantityAtStart) {
        if (!isWritingTransaction()) {
            return;
        }
        if (currentWatch().add(stock)) {
            stock.rememberQuantityAtStart(quantityAtStart);
        }
    }

    /**
     * Publishes the crossings of the current transaction: every watched row, plus the rows whose reservation alone
     * changed. Reservation changes are read first, because reading them flushes the persistence context.
     */
    private void publishCrossings(Set<Stock> watched) {
        Map<StockKey, BigDecimal> reservationChanges = new HashMap<>();
        for (ReservedStock change : reservationLookup.reservationChangesInCurrentTransaction()) {
            reservationChanges.put(new StockKey(change.productId(), change.warehouseId()), change.quantity());
        }
        Map<StockKey, Stock> rows = new LinkedHashMap<>();
        for (Stock stock : new ArrayList<>(watched)) {
            StockKey.of(stock).ifPresent(key -> rows.putIfAbsent(key, stock));
        }
        for (StockKey key : reservationChanges.keySet()) {
            if (!rows.containsKey(key)) {
                // No row means nothing on the shelf: a reservation alone cannot make it available.
                stockRepository.findByProductIdAndWarehouseId(key.productId(), key.warehouseId())
                        .ifPresent(stock -> rows.put(key, stock));
            }
        }
        rows.forEach((key, stock) -> crossingOf(stock, reservationChanges.getOrDefault(key, BigDecimal.ZERO))
                .ifPresent(eventPublisher::publishEvent));
    }

    /**
     * Availability change of one row, given how much the transaction changed its active reservations; empty when it
     * did not cross zero or is out of scope.
     */
    Optional<StockAvailabilityChangedEvent> crossingOf(Stock stock, BigDecimal reservationChange) {
        BigDecimal before = stock.quantityAtStart();
        BigDecimal after = stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity();
        Product product = stock.getProduct();
        Warehouse warehouse = stock.getWarehouse();
        if ((before.compareTo(after) == 0 && reservationChange.signum() == 0) || product == null || warehouse == null
                || !product.isStockTracked() || !warehouse.isSalesWarehouse()) {
            return Optional.empty();
        }
        BigDecimal reservedAfter = reservationLookup.activeReservation(product.getId(), warehouse.getId());
        BigDecimal reservedBefore = reservedAfter.subtract(reservationChange);
        StockAvailabilityChangedEvent event = new StockAvailabilityChangedEvent(
                warehouse.getCompany() == null ? null : warehouse.getCompany().getId(),
                product.getSku(), product.getName(), warehouse.getWarehouseNumber(), warehouse.getName(),
                available(before, reservedBefore), available(after, reservedAfter), OffsetDateTime.now());
        return event.wentOutOfStock() || event.cameBackInStock() ? Optional.of(event) : Optional.empty();
    }

    private static BigDecimal available(BigDecimal quantity, BigDecimal reserved) {
        return quantity.subtract(reserved).max(BigDecimal.ZERO);
    }

    private static boolean isWritingTransaction() {
        return TransactionSynchronizationManager.isSynchronizationActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly();
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
            if (!readOnly) {
                owner.publishCrossings(stocks);
            }
        }
    }

    private record StockKey(Long productId, Long warehouseId) {
        static Optional<StockKey> of(Stock stock) {
            return stock.getProduct() == null || stock.getWarehouse() == null ? Optional.empty()
                    : Optional.of(new StockKey(stock.getProduct().getId(), stock.getWarehouse().getId()));
        }
    }
}
