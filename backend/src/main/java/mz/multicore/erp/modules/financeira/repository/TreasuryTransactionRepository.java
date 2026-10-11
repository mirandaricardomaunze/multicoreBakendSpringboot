package mz.multicore.erp.modules.financeira.repository;

import mz.multicore.erp.modules.financeira.model.TreasuryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TreasuryTransactionRepository extends JpaRepository<TreasuryTransaction, Long> {
    List<TreasuryTransaction> findAllByOrderByTransactionDateDesc();
    List<TreasuryTransaction> findByTreasuryAccountCompanyIdOrderByTransactionDateDesc(Long companyId);
    Page<TreasuryTransaction> findByTreasuryAccountCompanyIdOrderByTransactionDateDesc(
            Long companyId, Pageable pageable);
    List<TreasuryTransaction> findByTreasuryAccountIdOrderByTransactionDateDesc(Long treasuryAccountId);
    List<TreasuryTransaction> findByTreasuryAccountIdAndTransactionDateBetween(Long treasuryAccountId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
