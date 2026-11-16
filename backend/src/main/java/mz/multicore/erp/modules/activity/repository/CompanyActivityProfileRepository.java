package mz.multicore.erp.modules.activity.repository;

import mz.multicore.erp.modules.activity.model.CompanyActivityProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyActivityProfileRepository extends JpaRepository<CompanyActivityProfile, Long> {

    List<CompanyActivityProfile> findByCompanyIdOrderById(Long companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CompanyActivityProfile p where p.company.id = :companyId")
    void deleteByCompanyId(@Param("companyId") Long companyId);
}
