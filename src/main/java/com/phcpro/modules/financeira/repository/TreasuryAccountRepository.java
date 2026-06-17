package com.phcpro.modules.financeira.repository;

import com.phcpro.modules.financeira.model.TreasuryAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreasuryAccountRepository extends JpaRepository<TreasuryAccount, Long> {
    List<TreasuryAccount> findByCompanyIdOrderByName(Long companyId);
    Optional<TreasuryAccount> findByIdAndCompanyId(Long id, Long companyId);
}
