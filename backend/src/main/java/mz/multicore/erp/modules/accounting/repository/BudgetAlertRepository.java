package mz.multicore.erp.modules.accounting.repository;

import mz.multicore.erp.modules.accounting.model.BudgetAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, Long> {

    boolean existsByCompanyIdAndCostCenterIdAndFiscalYearAndMonthAndThresholdPercent(
            Long companyId, Long costCenterId, int fiscalYear, int month, int thresholdPercent);

    List<BudgetAlert> findByCompanyIdAndFiscalYearOrderByMonthDescThresholdPercentDescIdDesc(
            Long companyId, int fiscalYear);
}
