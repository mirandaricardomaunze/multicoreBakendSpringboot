package mz.multicore.erp.modules.butchery.repository;

import mz.multicore.erp.modules.butchery.model.BreakdownTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BreakdownTemplateRepository extends JpaRepository<BreakdownTemplate, Long> {

    @Query("select distinct t from BreakdownTemplate t join fetch t.sourceProduct left join fetch t.lines l left join fetch l.product "
            + "where t.company.id = :companyId order by t.name")
    List<BreakdownTemplate> findByCompany(@Param("companyId") Long companyId);

    Optional<BreakdownTemplate> findByIdAndCompanyId(Long id, Long companyId);
}
