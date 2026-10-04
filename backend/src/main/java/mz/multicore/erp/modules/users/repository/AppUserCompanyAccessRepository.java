package mz.multicore.erp.modules.users.repository;

import mz.multicore.erp.modules.users.model.AppUserCompanyAccess;
import mz.multicore.erp.modules.users.model.AppUserCompanyAccessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserCompanyAccessRepository
        extends JpaRepository<AppUserCompanyAccess, AppUserCompanyAccessId> {

    long countByCompanyIdAndRoleIgnoreCase(Long companyId, String role);

    @Query("""
            select count(access)
              from AppUserCompanyAccess access
             where access.company.id = :companyId
               and upper(access.role) = upper(:role)
               and access.user.active = true
            """)
    long countActiveByCompanyIdAndRole(@Param("companyId") Long companyId,
                                       @Param("role") String role);

    long countByCompanyId(Long companyId);
}
