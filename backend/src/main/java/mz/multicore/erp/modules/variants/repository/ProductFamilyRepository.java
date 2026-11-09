package mz.multicore.erp.modules.variants.repository;

import mz.multicore.erp.modules.variants.model.ProductFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductFamilyRepository extends JpaRepository<ProductFamily, Long> {

    @Query("select distinct f from ProductFamily f join fetch f.templateProduct left join fetch f.variants v left join fetch v.product "
            + "where f.company.id = :companyId order by f.name")
    List<ProductFamily> findByCompany(@Param("companyId") Long companyId);

    Optional<ProductFamily> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndSkuPrefixIgnoreCase(Long companyId, String skuPrefix);
}
