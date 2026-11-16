package mz.multicore.erp.modules.customorders.repository;

import mz.multicore.erp.modules.customorders.model.OrderCustomization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderCustomizationRepository extends JpaRepository<OrderCustomization, Long> {

    @Query("select c from OrderCustomization c where c.order.id = :orderId and c.company.id = :companyId")
    Optional<OrderCustomization> findByOrder(@Param("orderId") Long orderId, @Param("companyId") Long companyId);

    @Query("select c from OrderCustomization c where c.order.id in :orderIds")
    List<OrderCustomization> findByOrders(@Param("orderIds") Collection<Long> orderIds);
}
