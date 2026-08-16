package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCompanyId(Long companyId);

    /** Encomendas que ainda não foram faturadas — usado pelo diálogo "Faturar Encomenda". */
    List<Order> findByCompanyIdAndStatusAndInvoiceIdIsNull(Long companyId, String status);
    Optional<Order> findByCompanyIdAndIdempotencyKey(Long companyId, String idempotencyKey);
}
