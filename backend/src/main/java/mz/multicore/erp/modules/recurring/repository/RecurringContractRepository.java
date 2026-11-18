package mz.multicore.erp.modules.recurring.repository;

import mz.multicore.erp.modules.recurring.model.ContractStatus;
import mz.multicore.erp.modules.recurring.model.RecurringContract;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringContractRepository extends JpaRepository<RecurringContract, Long> {

    @Query("SELECT c FROM RecurringContract c JOIN FETCH c.client WHERE c.company.id = :companyId ORDER BY c.contractNumber ASC")
    List<RecurringContract> findByCompanyIdWithClient(@Param("companyId") Long companyId);

    @Query("SELECT c FROM RecurringContract c JOIN FETCH c.client WHERE c.company.id = :companyId AND c.id = :id")
    Optional<RecurringContract> findByIdAndCompanyIdWithClient(@Param("companyId") Long companyId, @Param("id") Long id);

    boolean existsByCompanyIdAndContractNumber(Long companyId, String contractNumber);

    List<RecurringContract> findByCompanyIdAndStatus(Long companyId, ContractStatus status);

    @Query("SELECT c.id FROM RecurringContract c WHERE c.company.id = :companyId AND c.status = :status "
            + "AND c.nextBillingDate <= :referenceDate ORDER BY c.contractNumber ASC")
    List<Long> findDueContractIds(
            @Param("companyId") Long companyId,
            @Param("status") ContractStatus status,
            @Param("referenceDate") LocalDate referenceDate);

    /** Locks the contract while one billing period is issued, so two simultaneous runs cannot bill it twice. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM RecurringContract c JOIN FETCH c.client WHERE c.company.id = :companyId AND c.id = :id")
    Optional<RecurringContract> lockForBilling(@Param("companyId") Long companyId, @Param("id") Long id);
}
