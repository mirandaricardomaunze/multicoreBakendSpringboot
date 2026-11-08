package mz.multicore.erp.modules.projects.repository;

import mz.multicore.erp.modules.projects.model.CustomerProjectOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerProjectOrderRepository extends JpaRepository<CustomerProjectOrder, Long> {

    @Query("select l from CustomerProjectOrder l join fetch l.order o where l.project.id = :projectId order by o.id desc")
    List<CustomerProjectOrder> findByProjectWithOrder(@Param("projectId") Long projectId);

    Optional<CustomerProjectOrder> findByOrderId(Long orderId);
}
