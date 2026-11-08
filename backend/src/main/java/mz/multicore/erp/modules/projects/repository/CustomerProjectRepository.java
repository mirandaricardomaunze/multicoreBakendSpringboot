package mz.multicore.erp.modules.projects.repository;

import mz.multicore.erp.modules.projects.model.CustomerProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerProjectRepository extends JpaRepository<CustomerProject, Long> {

    @Query("select p from CustomerProject p join fetch p.client where p.company.id = :companyId "
            + "and (:clientId is null or p.client.id = :clientId) order by p.active desc, p.name")
    List<CustomerProject> findByCompany(@Param("companyId") Long companyId, @Param("clientId") Long clientId);

    Optional<CustomerProject> findByIdAndCompanyId(Long id, Long companyId);

    long countByCompanyId(Long companyId);
}
