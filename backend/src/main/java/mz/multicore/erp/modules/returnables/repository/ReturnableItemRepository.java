package mz.multicore.erp.modules.returnables.repository;

import mz.multicore.erp.modules.returnables.model.ReturnableItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnableItemRepository extends JpaRepository<ReturnableItem, Long> {

    @Query("select i from ReturnableItem i join fetch i.product where i.company.id = :companyId order by i.product.name")
    List<ReturnableItem> findByCompany(@Param("companyId") Long companyId);

    Optional<ReturnableItem> findByIdAndCompanyId(Long id, Long companyId);

    Optional<ReturnableItem> findByCompanyIdAndProductId(Long companyId, Long productId);
}
