package mz.multicore.erp.modules.inventory.service;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import mz.multicore.erp.modules.inventory.model.Stock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * JPA listener on {@link Stock} (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1, WH-02): every stock row a writing
 * transaction reads or creates is handed to the {@link StockAvailabilityTracker}, which compares the quantity at the
 * start with the one being committed. One listener instead of one call per {@code setQuantity}: sales, purchases,
 * transfers, transit, adjustments and counts are all covered, including future paths. A created row starts at zero
 * (no row = nothing available).
 *
 * <p>The tracker comes through an {@link ObjectProvider}: slices of the context without it (JPA-only tests) still
 * build the entity manager and simply track nothing.
 */
@Component
public class StockAvailabilityPublisher {

    private final ObjectProvider<StockAvailabilityTracker> tracker;

    public StockAvailabilityPublisher(ObjectProvider<StockAvailabilityTracker> tracker) {
        this.tracker = tracker;
    }

    @PostLoad
    public void loaded(Stock stock) {
        tracker.ifAvailable(t -> t.watch(stock, stock.getQuantity()));
    }

    @PostPersist
    public void created(Stock stock) {
        tracker.ifAvailable(t -> t.watch(stock, BigDecimal.ZERO));
    }
}
