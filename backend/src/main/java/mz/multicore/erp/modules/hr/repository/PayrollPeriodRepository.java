package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    Optional<PayrollPeriod> findByCompanyIdAndYearAndMonth(Long companyId, int year, int month);

    List<PayrollPeriod> findByCompanyIdOrderByYearDescMonthDesc(Long companyId);
}
