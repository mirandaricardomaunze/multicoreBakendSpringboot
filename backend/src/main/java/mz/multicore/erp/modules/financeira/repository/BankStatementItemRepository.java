package mz.multicore.erp.modules.financeira.repository;

import mz.multicore.erp.modules.financeira.model.BankStatementItem;
import mz.multicore.erp.modules.financeira.model.BankStatementItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankStatementItemRepository extends JpaRepository<BankStatementItem, Long> {
    List<BankStatementItem> findByBankStatementIdOrderByTransactionDateAscIdAsc(Long bankStatementId);
    List<BankStatementItem> findByBankStatementIdAndStatus(Long bankStatementId, BankStatementItemStatus status);
    Optional<BankStatementItem> findByIdAndBankStatementCompanyId(Long id, Long companyId);
}
