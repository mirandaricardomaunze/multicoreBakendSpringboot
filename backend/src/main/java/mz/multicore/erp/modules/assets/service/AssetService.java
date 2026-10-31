package mz.multicore.erp.modules.assets.service;

import mz.multicore.erp.modules.assets.dto.AssetDTO;
import mz.multicore.erp.modules.assets.dto.CreateAssetRequest;
import mz.multicore.erp.modules.assets.dto.DepreciationScheduleDTO;
import mz.multicore.erp.modules.assets.repository.AssetDepreciationRecordRepository;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssetService {

    private final AssetLifecycleService lifecycleService;
    private final AssetDepreciationBatchService depreciationBatchService;

    @Autowired
    public AssetService(
            AssetLifecycleService lifecycleService,
            AssetDepreciationBatchService depreciationBatchService
    ) {
        this.lifecycleService = lifecycleService;
        this.depreciationBatchService = depreciationBatchService;
    }

    public AssetService(
            AssetRepository assetRepository,
            AssetDepreciationRecordRepository depreciationRepository,
            CompanyRepository companyRepository,
            AssetDepreciationEngine depreciationEngine
    ) {
        this(assetRepository, depreciationRepository, companyRepository, depreciationEngine, null);
    }

    public AssetService(
            AssetRepository assetRepository,
            AssetDepreciationRecordRepository depreciationRepository,
            CompanyRepository companyRepository,
            AssetDepreciationEngine depreciationEngine,
            ApplicationEventPublisher eventPublisher
    ) {
        this(
                new AssetLifecycleService(assetRepository, companyRepository),
                new AssetDepreciationBatchService(assetRepository, depreciationRepository, companyRepository, depreciationEngine, eventPublisher)
        );
    }

    @Transactional
    public AssetDTO createAsset(Long companyId, CreateAssetRequest request) {
        return lifecycleService.createAsset(companyId, request);
    }

    @Transactional(readOnly = true)
    public List<AssetDTO> listAssets(Long companyId) {
        return lifecycleService.listAssets(companyId);
    }

    @Transactional(readOnly = true)
    public AssetDTO getAsset(Long companyId, Long id) {
        return lifecycleService.getAsset(companyId, id);
    }

    @Transactional
    public List<DepreciationScheduleDTO> runMonthlyDepreciation(Long companyId, int year, int month) {
        return depreciationBatchService.runMonthlyDepreciation(companyId, year, month);
    }

    @Transactional(readOnly = true)
    public List<DepreciationScheduleDTO> getDepreciationHistory(Long companyId, Long assetId) {
        return depreciationBatchService.getDepreciationHistory(companyId, assetId);
    }
}
