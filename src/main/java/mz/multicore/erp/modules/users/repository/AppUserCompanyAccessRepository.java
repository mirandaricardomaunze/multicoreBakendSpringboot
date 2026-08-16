package mz.multicore.erp.modules.users.repository;

import mz.multicore.erp.modules.users.model.AppUserCompanyAccess;
import mz.multicore.erp.modules.users.model.AppUserCompanyAccessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserCompanyAccessRepository
        extends JpaRepository<AppUserCompanyAccess, AppUserCompanyAccessId> {

    long countByCompanyIdAndRoleIgnoreCase(Long companyId, String role);

    long countByCompanyId(Long companyId);
}
