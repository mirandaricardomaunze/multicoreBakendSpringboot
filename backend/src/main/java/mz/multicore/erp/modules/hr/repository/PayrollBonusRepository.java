package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.PayrollBonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollBonusRepository extends JpaRepository<PayrollBonus, Long> {

    boolean existsByEmployeeIdAndBonusTypeAndYear(Long employeeId, String bonusType, int year);

    boolean existsByBonusTypeAndReferenceId(String bonusType, long referenceId);

    @Query("SELECT b FROM PayrollBonus b JOIN FETCH b.employee e WHERE e.company.id = :companyId "
            + "ORDER BY b.id DESC")
    List<PayrollBonus> findAllWithEmployeeByCompanyId(@Param("companyId") Long companyId);
}
