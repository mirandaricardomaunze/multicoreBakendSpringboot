package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.multicore.erp.modules.comercial.model.ReservedStock;
import java.math.BigDecimal;
import java.util.List;

/**
 * Read port of the stock that customer orders hold reserved (docs/MODULE_DECOUPLING_SPEC.md §2): other modules learn
 * what is promised without touching the order repositories. Used for the public API's available stock (API-07) and the
 * stock availability webhooks (WH-02).
 */
@Service
@Transactional(readOnly = true)
public class OrderReservationLookup {

    private final OrderLineRepository orderLineRepository;
    private final OrderReservationWatcher reservationWatcher;

    public OrderReservationLookup(OrderLineRepository orderLineRepository, OrderReservationWatcher reservationWatcher) {
        this.orderLineRepository = orderLineRepository;
        this.reservationWatcher = reservationWatcher;
    }

    /** Quantity of one product held by active order reservations in one warehouse (zero when none). */
    public BigDecimal activeReservation(Long productId, Long warehouseId) {
        BigDecimal reserved = orderLineRepository.sumActiveReservations(productId, warehouseId);
        return reserved == null ? BigDecimal.ZERO : reserved;
    }

    /**
     * Net change of active reservations made by the current writing transaction, per product × warehouse (after −
     * start, only non-zero; {@code quantity} is the change). Flushes first so freed and removed lines count. Used by
     * the inventory, just before the commit, to publish reservation-only availability changes (WH-02).
     */
    public List<ReservedStock> reservationChangesInCurrentTransaction() {
        if (!reservationWatcher.isWatching()) {
            return List.of();
        }
        orderLineRepository.flush();
        return reservationWatcher.changesOfCurrentTransaction();
    }

    /** Active reservations of the company's orders, summed per product and warehouse. */
    public List<ReservedStock> activeReservationsForCompany(Long companyId) {
        return orderLineRepository.sumActiveReservationsByCompany(companyId);
    }
}
