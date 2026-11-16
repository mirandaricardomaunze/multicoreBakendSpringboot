package mz.multicore.erp.modules.assets.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.assets.dto.AssetDTO;
import mz.multicore.erp.modules.assets.dto.CreateAssetRequest;
import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.model.AssetStatus;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AssetLifecycleService {

    private final AssetRepository assetRepository;
    private final CompanyLookup companyLookup;

    public AssetLifecycleService(AssetRepository assetRepository, CompanyLookup companyLookup) {
        this.assetRepository = assetRepository;
        this.companyLookup = companyLookup;
    }

    public AssetDTO createAsset(Long companyId, CreateAssetRequest request) {
        if (companyId == null) {
            throw new BusinessRuleException("A empresa activa é obrigatória.");
        }
        if (assetRepository.existsByCompanyIdAndAssetCode(companyId, request.assetCode())) {
            throw new BusinessRuleException("Já existe um activo registado com o código '" + request.assetCode() + "'.");
        }

        Company company = companyLookup.require(companyId);

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

    public List<AssetDTO> listAssets(Long companyId) {
        if (companyId == null) return List.of();
        return assetRepository.findByCompanyIdOrderByAssetCodeAsc(companyId).stream().map(this::toDTO).toList();
    }

    public AssetDTO getAsset(Long companyId, Long id) {
        Asset asset = assetRepository.findById(id)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new BusinessRuleException("Activo não encontrado ou não pertence a esta empresa."));
        return toDTO(asset);
    }

    public AssetDTO toDTO(Asset a) {
        return new AssetDTO(
                a.getId(), a.getCompany().getId(), a.getAssetCode(), a.getName(),
                a.getCategory(), a.getStatus(), a.getAcquisitionDate(),
                a.getAcquisitionCost(), a.getResidualValue(), a.getAnnualDepreciationRate(),
                a.getAccumulatedDepreciation(), a.getNetBookValue(),
                a.getSerialNumber(), a.getLocation(), a.getAssignedEmployee()
        );
    }
}
