package com.phcpro.modules.financeira.repository;

import com.phcpro.modules.financeira.model.TreasuryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TreasuryTransactionRepository extends JpaRepository<TreasuryTransaction, Long> {
    List<TreasuryTransaction> findAllByOrderByTransactionDateDesc();
}
