package com.phcpro.modules.financeira.repository;

import com.phcpro.modules.financeira.model.TreasuryAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TreasuryAccountRepository extends JpaRepository<TreasuryAccount, Long> {
}
