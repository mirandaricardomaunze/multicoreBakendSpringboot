package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    @Query("select coalesce(sum(l.reservedQuantity), 0) from OrderLine l where l.product.id = :productId " +
            "and l.order.warehouse.id = :warehouseId and l.order.reservationActive = true")
    BigDecimal sumActiveReservations(@Param("productId") Long productId,
                                     @Param("warehouseId") Long warehouseId);
}
