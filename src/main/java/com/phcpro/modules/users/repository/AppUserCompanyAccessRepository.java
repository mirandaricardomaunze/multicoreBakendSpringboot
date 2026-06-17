package com.phcpro.modules.users.repository;

import com.phcpro.modules.users.model.AppUserCompanyAccess;
import com.phcpro.modules.users.model.AppUserCompanyAccessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserCompanyAccessRepository
        extends JpaRepository<AppUserCompanyAccess, AppUserCompanyAccessId> {

    long countByCompanyIdAndRoleIgnoreCase(Long companyId, String role);
}
