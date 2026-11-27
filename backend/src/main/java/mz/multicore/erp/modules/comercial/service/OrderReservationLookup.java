package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.repository.OrderLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.multicore.erp.modules.comercial.model.ReservedStock;
import java.math.BigDecimal;
import java.util.List;

/**
 * Read port of the stock that customer orders hold reserved (docs/MODULE_DECOUPLING_SPEC.md §2): other modules learn
 * what is promised without touching the order repositories. Used for the public API's available stock (API-07).
 */
@Service
@Transactional(readOnly = true)
public class OrderReservationLookup {

    private final OrderLineRepository orderLineRepository;

    public OrderReservationLookup(OrderLineRepository orderLineRepository) {
        this.orderLineRepository = orderLineRepository;
    }

    /** Quantity of one product held by active order reservations in one warehouse (zero when none). */
    public BigDecimal activeReservation(Long productId, Long warehouseId) {
        BigDecimal reserved = orderLineRepository.sumActiveReservations(productId, warehouseId);
        return reserved == null ? BigDecimal.ZERO : reserved;
    }

    /** Active reservations of the company's orders, summed per product and warehouse. */
    public List<ReservedStock> activeReservationsForCompany(Long companyId) {
        return orderLineRepository.sumActiveReservationsByCompany(companyId);
    }
}
