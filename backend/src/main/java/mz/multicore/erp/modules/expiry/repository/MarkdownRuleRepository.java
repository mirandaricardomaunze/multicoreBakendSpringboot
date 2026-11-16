package mz.multicore.erp.modules.expiry.repository;

import mz.multicore.erp.modules.expiry.model.MarkdownRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkdownRuleRepository extends JpaRepository<MarkdownRule, Long> {

    @Query("select r from MarkdownRule r left join fetch r.category where r.company.id = :companyId order by r.daysBefore desc, r.id")
    List<MarkdownRule> findByCompany(@Param("companyId") Long companyId);

    Optional<MarkdownRule> findByIdAndCompanyId(Long id, Long companyId);
}
