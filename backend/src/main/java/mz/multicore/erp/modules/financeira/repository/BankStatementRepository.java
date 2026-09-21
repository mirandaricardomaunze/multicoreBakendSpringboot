package mz.multicore.erp.modules.financeira.repository;

import mz.multicore.erp.modules.financeira.model.BankReconciliationStatus;
import mz.multicore.erp.modules.financeira.model.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {
    List<BankStatement> findByCompanyIdOrderByStartDateDesc(Long companyId);
    List<BankStatement> findByCompanyIdAndTreasuryAccountIdOrderByStartDateDesc(Long companyId, Long treasuryAccountId);
    List<BankStatement> findByCompanyIdAndStatus(Long companyId, BankReconciliationStatus status);
    Optional<BankStatement> findByIdAndCompanyId(Long id, Long companyId);
}
