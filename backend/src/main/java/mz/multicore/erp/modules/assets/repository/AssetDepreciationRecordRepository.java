package mz.multicore.erp.modules.assets.repository;

import mz.multicore.erp.modules.assets.model.AssetDepreciationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetDepreciationRecordRepository extends JpaRepository<AssetDepreciationRecord, Long> {

    List<AssetDepreciationRecord> findByCompanyIdAndAssetIdOrderByPeriodYearDescPeriodMonthDesc(Long companyId, Long assetId);

    List<AssetDepreciationRecord> findByCompanyIdAndPeriodYearAndPeriodMonth(Long companyId, int periodYear, int periodMonth);

    Optional<AssetDepreciationRecord> findByCompanyIdAndAssetIdAndPeriodYearAndPeriodMonth(
            Long companyId, Long assetId, int periodYear, int periodMonth);

    boolean existsByCompanyIdAndAssetIdAndPeriodYearAndPeriodMonth(
            Long companyId, Long assetId, int periodYear, int periodMonth);
}
