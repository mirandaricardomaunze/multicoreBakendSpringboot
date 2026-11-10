package mz.multicore.erp.modules.pickup.repository;

import jakarta.persistence.LockModeType;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.pickup.model.PickupAccount;
import mz.multicore.erp.modules.pickup.model.PickupAccountStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PickupAccountRepository extends JpaRepository<PickupAccount, Long> {

    @Query("select a from PickupAccount a join fetch a.invoice i left join fetch i.client "
            + "where a.company.id = :companyId order by a.id desc")
    List<PickupAccount> findByCompany(@Param("companyId") Long companyId);

    @Query("select a from PickupAccount a join fetch a.invoice i left join fetch i.client "
            + "where a.company.id = :companyId and a.status = :status order by a.id desc")
    List<PickupAccount> findByCompanyAndStatus(@Param("companyId") Long companyId,
                                               @Param("status") PickupAccountStatus status);

    Optional<PickupAccount> findByIdAndCompanyId(Long id, Long companyId);

    /** Serialises concurrent pickups on the same account so two slips can never spend the same balance. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PickupAccount a where a.id = :id and a.company.id = :companyId")
    Optional<PickupAccount> lockByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    boolean existsByInvoiceId(Long invoiceId);

    Optional<PickupAccount> findByInvoiceId(Long invoiceId);

    @Query("select i from Invoice i left join fetch i.client where i.company.id = :companyId "
            + "and i.status in :statuses and i.deliveryGuideId is null "
            + "and not exists (select a.id from PickupAccount a where a.invoice = i) "
            + "and (:search = '' or lower(i.invoiceNumber) like :search or lower(i.customerName) like :search) "
            + "order by i.id desc")
    List<Invoice> findEligibleInvoices(@Param("companyId") Long companyId,
                                       @Param("statuses") Collection<InvoiceStatus> statuses,
                                       @Param("search") String search, Pageable page);
}
