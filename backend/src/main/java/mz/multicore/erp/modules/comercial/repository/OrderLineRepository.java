package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import mz.multicore.erp.modules.comercial.model.ReservedStock;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    @Query("select coalesce(sum(l.reservedQuantity), 0) from OrderLine l where l.product.id = :productId " +
            "and l.order.warehouse.id = :warehouseId and l.order.reservationActive = true")
    BigDecimal sumActiveReservations(@Param("productId") Long productId,
                                     @Param("warehouseId") Long warehouseId);

    @Query("select new mz.multicore.erp.modules.comercial.model.ReservedStock(l.product.id, l.order.warehouse.id, " +
            "coalesce(sum(l.reservedQuantity), 0)) from OrderLine l where l.order.company.id = :companyId " +
            "and l.order.reservationActive = true and l.order.warehouse is not null " +
            "group by l.product.id, l.order.warehouse.id")
    List<ReservedStock> sumActiveReservationsByCompany(@Param("companyId") Long companyId);
}
