package com.phcpro.modules.users.repository;

import com.phcpro.modules.users.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    List<AppUser> findDistinctByCompanyAccessesCompanyIdOrderByName(Long companyId);
}
