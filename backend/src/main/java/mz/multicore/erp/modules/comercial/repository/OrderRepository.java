package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCompanyId(Long companyId);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT o FROM Order o LEFT JOIN FETCH o.client LEFT JOIN FETCH o.businessUnit WHERE o.company.id = :companyId ORDER BY o.createdAt DESC",
            countQuery = "SELECT count(o) FROM Order o WHERE o.company.id = :companyId")
    org.springframework.data.domain.Page<Order> findPageByCompanyId(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            org.springframework.data.domain.Pageable pageable);

    /** Encomendas que ainda não foram faturadas — usado pelo diálogo "Faturar Encomenda". */
    List<Order> findByCompanyIdAndStatusAndInvoiceIdIsNull(Long companyId, String status);
    Optional<Order> findByCompanyIdAndIdempotencyKey(Long companyId, String idempotencyKey);
    Optional<Order> findFirstByCompanyIdAndInvoiceId(Long companyId, Long invoiceId);
    Optional<Order> findFirstByInvoiceId(Long invoiceId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.id = :id AND o.company.id = :companyId")
    Optional<Order> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id,
                                  @org.springframework.data.repository.query.Param("companyId") Long companyId);
}
