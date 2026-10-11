package mz.multicore.erp.modules.assets.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.assets.dto.AssetDTO;
import mz.multicore.erp.modules.assets.dto.CreateAssetRequest;
import mz.multicore.erp.modules.assets.dto.DepreciationScheduleDTO;
import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.model.AssetDepreciationRecord;
import mz.multicore.erp.modules.assets.model.AssetStatus;
import mz.multicore.erp.modules.assets.repository.AssetDepreciationRecordRepository;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetDepreciationRecordRepository depreciationRepository;
    private final CompanyRepository companyRepository;
    private final AssetDepreciationEngine depreciationEngine;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public AssetService(AssetRepository assetRepository,
                        AssetDepreciationRecordRepository depreciationRepository,
                        CompanyRepository companyRepository,
                        AssetDepreciationEngine depreciationEngine) {
        this(assetRepository, depreciationRepository, companyRepository, depreciationEngine, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AssetService(AssetRepository assetRepository,
                        AssetDepreciationRecordRepository depreciationRepository,
                        CompanyRepository companyRepository,
                        AssetDepreciationEngine depreciationEngine,
                        org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.assetRepository = assetRepository;
        this.depreciationRepository = depreciationRepository;
        this.companyRepository = companyRepository;
        this.depreciationEngine = depreciationEngine;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AssetDTO createAsset(Long companyId, CreateAssetRequest request) {
        if (companyId == null) {
            throw new BusinessRuleException("A empresa activa é obrigatória.");
        }
        if (assetRepository.existsByCompanyIdAndAssetCode(companyId, request.assetCode())) {
            throw new BusinessRuleException("Já existe um activo registado com o código '" + request.assetCode() + "'.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada: " + companyId));

        BigDecimal rate = request.annualDepreciationRate();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            rate = BigDecimal.valueOf(request.category().defaultAnnualRate());
        }

        BigDecimal residual = request.residualValue() != null ? request.residualValue() : BigDecimal.ZERO;
        if (residual.compareTo(request.acquisitionCost()) >= 0) {
            throw new BusinessRuleException("O valor residual não pode ser igual ou superior ao custo de aquisição.");
        }

        Asset asset = new Asset();
        asset.setCompany(company);
        asset.setAssetCode(request.assetCode().trim().toUpperCase());
        asset.setName(request.name().trim());
        asset.setCategory(request.category());
        asset.setStatus(AssetStatus.ACTIVE);
        asset.setAcquisitionDate(request.acquisitionDate());
        asset.setAcquisitionCost(request.acquisitionCost());
        asset.setResidualValue(residual);
        asset.setAnnualDepreciationRate(rate);
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setNetBookValue(request.acquisitionCost());
        asset.setSerialNumber(request.serialNumber());
        asset.setLocation(request.location());
        asset.setAssignedEmployee(request.assignedEmployee());

        Asset saved = assetRepository.save(asset);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<AssetDTO> listAssets(Long companyId) {
        if (companyId == null) return List.of();
        return assetRepository.findByCompanyIdOrderByAssetCodeAsc(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetDTO getAsset(Long companyId, Long id) {
        Asset asset = assetRepository.findById(id)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new BusinessRuleException("Activo não encontrado ou não pertence a esta empresa."));
        return toDTO(asset);
    }

    @Transactional
    public List<DepreciationScheduleDTO> runMonthlyDepreciation(Long companyId, int year, int month) {
        if (companyId == null) {
            throw new BusinessRuleException("A empresa activa é obrigatória.");
        }
        if (month < 1 || month > 12) {
            throw new BusinessRuleException("Mês inválido para cálculo de depreciações: " + month);
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada: " + companyId));

        List<Asset> activeAssets = assetRepository.findByCompanyIdOrderByAssetCodeAsc(companyId)
                .stream()
                .filter(a -> a.getStatus() == AssetStatus.ACTIVE || a.getStatus() == AssetStatus.DEPRECIATING)
                .toList();

        List<DepreciationScheduleDTO> results = new ArrayList<>();
        LocalDate calcDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1); // Último dia do mês

        for (Asset asset : activeAssets) {
            // Se já foi depreciado neste período, ignora
            if (depreciationRepository.existsByCompanyIdAndAssetIdAndPeriodYearAndPeriodMonth(companyId, asset.getId(), year, month)) {
                continue;
            }

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
                eventPublisher.publishEvent(new mz.multicore.erp.architecture.events.AssetDepreciatedEvent(
                        companyId,
                        asset.getId(),
                        asset.getAssetCode(),
                        calcDate,
                        quota
                ));
            }

            results.add(new DepreciationScheduleDTO(
                    asset.getId(),
                    asset.getAssetCode(),
                    year,
                    month,
                    calcDate,
                    opening,
                    quota,
                    closing,
                    newAccum
            ));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<DepreciationScheduleDTO> getDepreciationHistory(Long companyId, Long assetId) {
        return depreciationRepository.findByCompanyIdAndAssetIdOrderByPeriodYearDescPeriodMonthDesc(companyId, assetId)
                .stream()
                .map(r -> new DepreciationScheduleDTO(
                        r.getAsset().getId(),
                        r.getAsset().getAssetCode(),
                        r.getPeriodYear(),
                        r.getPeriodMonth(),
                        r.getCalculationDate(),
                        r.getOpeningBookValue(),
                        r.getDepreciationAmount(),
                        r.getClosingBookValue(),
                        r.getAccumulatedDepreciation()
                ))
                .toList();
    }

    private AssetDTO toDTO(Asset a) {
        return new AssetDTO(
                a.getId(),
                a.getCompany().getId(),
                a.getAssetCode(),
                a.getName(),
                a.getCategory(),
                a.getStatus(),
                a.getAcquisitionDate(),
                a.getAcquisitionCost(),
                a.getResidualValue(),
                a.getAnnualDepreciationRate(),
                a.getAccumulatedDepreciation(),
                a.getNetBookValue(),
                a.getSerialNumber(),
                a.getLocation(),
                a.getAssignedEmployee()
        );
    }
}
