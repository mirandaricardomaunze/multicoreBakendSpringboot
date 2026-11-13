package mz.multicore.erp.modules.assets.service;

import mz.multicore.erp.architecture.events.AssetDepreciatedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.assets.dto.DepreciationScheduleDTO;
import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.model.AssetDepreciationRecord;
import mz.multicore.erp.modules.assets.model.AssetStatus;
import mz.multicore.erp.modules.assets.repository.AssetDepreciationRecordRepository;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class AssetDepreciationBatchService {

    private final AssetRepository assetRepository;
    private final AssetDepreciationRecordRepository depreciationRepository;
    private final CompanyLookup companyLookup;
    private final AssetDepreciationEngine depreciationEngine;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public AssetDepreciationBatchService(
            AssetRepository assetRepository,
            AssetDepreciationRecordRepository depreciationRepository,
            CompanyLookup companyLookup,
            AssetDepreciationEngine depreciationEngine,
            ApplicationEventPublisher eventPublisher
    ) {
        this.assetRepository = assetRepository;
        this.depreciationRepository = depreciationRepository;
        this.companyLookup = companyLookup;
        this.depreciationEngine = depreciationEngine;
        this.eventPublisher = eventPublisher;
    }

    public List<DepreciationScheduleDTO> runMonthlyDepreciation(Long companyId, int year, int month) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        if (month < 1 || month > 12) throw new BusinessRuleException("Mês inválido para cálculo de depreciações: " + month);

        Company company = companyLookup.require(companyId);

        List<Asset> activeAssets = assetRepository.findByCompanyIdOrderByAssetCodeAsc(companyId).stream()
                .filter(a -> a.getStatus() == AssetStatus.ACTIVE || a.getStatus() == AssetStatus.DEPRECIATING)
                .toList();

        List<DepreciationScheduleDTO> results = new ArrayList<>();
        LocalDate calcDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);

        for (Asset asset : activeAssets) {
            if (depreciationRepository.existsByCompanyIdAndAssetIdAndPeriodYearAndPeriodMonth(companyId, asset.getId(), year, month)) {
                continue;
            }
            // An asset only depreciates from the month it was acquired: a run for May never touches a June purchase.
            if (asset.getAcquisitionDate() != null && asset.getAcquisitionDate().isAfter(calcDate)) continue;

            BigDecimal quota = depreciationEngine.calculateMonthlyDepreciation(asset);
            if (quota.compareTo(BigDecimal.ZERO) <= 0) {
                if (asset.getNetBookValue().compareTo(asset.getResidualValue()) <= 0) {
                    asset.setStatus(AssetStatus.FULLY_DEPRECIATED);
                    assetRepository.save(asset);
                }
                continue;
            }

            BigDecimal opening = asset.getNetBookValue();
            BigDecimal newAccum = asset.getAccumulatedDepreciation().add(quota);
            BigDecimal closing = asset.getAcquisitionCost().subtract(newAccum);

            asset.setAccumulatedDepreciation(newAccum);
            asset.setNetBookValue(closing);
            asset.setStatus(closing.compareTo(asset.getResidualValue()) <= 0 ? AssetStatus.FULLY_DEPRECIATED : AssetStatus.DEPRECIATING);
            assetRepository.save(asset);

            AssetDepreciationRecord record = new AssetDepreciationRecord();
            record.setCompany(company);
            record.setAsset(asset);
            record.setPeriodYear(year);
            record.setPeriodMonth(month);
            record.setCalculationDate(calcDate);
            record.setOpeningBookValue(opening);
            record.setDepreciationAmount(quota);
            record.setClosingBookValue(closing);
            record.setAccumulatedDepreciation(newAccum);
            depreciationRepository.save(record);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new AssetDepreciatedEvent(companyId, asset.getId(), asset.getAssetCode(), calcDate, quota));
            }

            results.add(new DepreciationScheduleDTO(asset.getId(), asset.getAssetCode(), year, month, calcDate, opening, quota, closing, newAccum));
        }

        return results;
    }

    public List<DepreciationScheduleDTO> getDepreciationHistory(Long companyId, Long assetId) {
        return depreciationRepository.findByCompanyIdAndAssetIdOrderByPeriodYearDescPeriodMonthDesc(companyId, assetId)
                .stream()
                .map(r -> new DepreciationScheduleDTO(
                        r.getAsset().getId(), r.getAsset().getAssetCode(),
                        r.getPeriodYear(), r.getPeriodMonth(), r.getCalculationDate(),
                        r.getOpeningBookValue(), r.getDepreciationAmount(),
                        r.getClosingBookValue(), r.getAccumulatedDepreciation()
                ))
                .toList();
    }
}
