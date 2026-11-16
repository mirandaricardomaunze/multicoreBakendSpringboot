package mz.multicore.erp.modules.advances.repository;

import jakarta.persistence.LockModeType;
import mz.multicore.erp.modules.advances.model.CustomerAdvance;
import mz.multicore.erp.modules.advances.model.CustomerAdvanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAdvanceRepository extends JpaRepository<CustomerAdvance, Long> {

    @Query("""
            select a from CustomerAdvance a join fetch a.client join fetch a.advanceInvoice join fetch a.receipt
            left join fetch a.order left join fetch a.appliedInvoice left join fetch a.creditNote left join fetch a.offsetReceipt
            where a.company.id = :companyId
              and (:status is null or a.status = :status)
              and (:clientId is null or a.client.id = :clientId)
            order by a.createdAt desc, a.id desc""")
    List<CustomerAdvance> findByCompany(@Param("companyId") Long companyId,
                                        @Param("status") CustomerAdvanceStatus status,
                                        @Param("clientId") Long clientId);

    Optional<CustomerAdvance> findByCompanyIdAndRequestKey(Long companyId, String requestKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CustomerAdvance a where a.id = :id and a.company.id = :companyId")
    Optional<CustomerAdvance> lockByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    boolean existsByAdvanceInvoiceId(Long invoiceId);
}
