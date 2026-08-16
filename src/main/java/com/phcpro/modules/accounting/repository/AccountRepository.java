package com.phcpro.modules.accounting.repository;

import com.phcpro.modules.accounting.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByCompanyIdOrderByCode(Long companyId);

    Optional<Account> findByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyId(Long companyId);
}
