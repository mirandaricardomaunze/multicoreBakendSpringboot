package mz.multicore.erp.modules.assets.repository;

import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.model.AssetCategory;
import mz.multicore.erp.modules.assets.model.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findByCompanyIdOrderByAssetCodeAsc(Long companyId);

    List<Asset> findByCompanyIdAndStatusOrderByAssetCodeAsc(Long companyId, AssetStatus status);

    List<Asset> findByCompanyIdAndCategoryOrderByAssetCodeAsc(Long companyId, AssetCategory category);

    Optional<Asset> findByCompanyIdAndAssetCode(Long companyId, String assetCode);

    boolean existsByCompanyIdAndAssetCode(Long companyId, String assetCode);
}
