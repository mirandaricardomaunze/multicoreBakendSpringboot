package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.PayrollLiability;
import mz.multicore.erp.modules.hr.model.PayrollLiabilityStatus;
import mz.multicore.erp.modules.hr.model.PayrollLiabilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollLiabilityRepository extends JpaRepository<PayrollLiability, Long> {

    Optional<PayrollLiability> findByCompanyIdAndYearAndMonthAndLiabilityType(
            Long companyId, int year, int month, PayrollLiabilityType liabilityType);

    Optional<PayrollLiability> findByIdAndCompanyId(Long id, Long companyId);

    @Query("SELECT l FROM PayrollLiability l WHERE l.company.id = :companyId "
            + "ORDER BY l.year DESC, l.month DESC, l.liabilityType")
    List<PayrollLiability> findAllByCompany(@Param("companyId") Long companyId);

    @Query("SELECT l FROM PayrollLiability l WHERE l.company.id = :companyId AND l.status = :status "
            + "ORDER BY CASE WHEN l.dueDate IS NULL THEN 1 ELSE 0 END, l.dueDate, l.liabilityType")
    List<PayrollLiability> findByStatus(@Param("companyId") Long companyId,
                                        @Param("status") PayrollLiabilityStatus status);
}
