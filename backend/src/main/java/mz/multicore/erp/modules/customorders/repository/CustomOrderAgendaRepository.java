package mz.multicore.erp.modules.customorders.repository;

import mz.multicore.erp.modules.comercial.model.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** Read-only agenda: orders with a delivery date in the period, and the advances already paid for them. */
public interface CustomOrderAgendaRepository extends org.springframework.data.repository.Repository<Order, Long> {

    @Query("select distinct o from Order o left join fetch o.client left join fetch o.lines l left join fetch l.product "
            + "where o.company.id = :companyId and o.expectedDeliveryDate >= :from and o.expectedDeliveryDate <= :to "
            + "and o.status <> 'CANCELLED' order by o.expectedDeliveryDate, o.id")
    List<Order> agenda(@Param("companyId") Long companyId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select a.order.id, sum(a.amount) from mz.multicore.erp.modules.advances.model.CustomerAdvance a "
            + "where a.order.id in :orderIds group by a.order.id")
    List<Object[]> advancesByOrder(@Param("orderIds") Collection<Long> orderIds);

    default java.util.Map<Long, BigDecimal> advances(Collection<Long> orderIds) {
        java.util.Map<Long, BigDecimal> result = new java.util.HashMap<>();
        if (orderIds.isEmpty()) return result;
        for (Object[] row : advancesByOrder(orderIds)) result.put((Long) row[0], (BigDecimal) row[1]);
        return result;
    }
}
