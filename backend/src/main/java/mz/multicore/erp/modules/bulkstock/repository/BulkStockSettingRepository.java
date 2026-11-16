package mz.multicore.erp.modules.bulkstock.repository;

import mz.multicore.erp.modules.bulkstock.model.BulkStockSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulkStockSettingRepository extends JpaRepository<BulkStockSetting, Long> {

    @Query("select s from BulkStockSetting s join fetch s.product p where s.company.id = :companyId order by p.name")
    List<BulkStockSetting> findByCompany(@Param("companyId") Long companyId);

    Optional<BulkStockSetting> findByCompanyIdAndProductId(Long companyId, Long productId);
}
