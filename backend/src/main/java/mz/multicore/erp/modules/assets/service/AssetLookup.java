package mz.multicore.erp.modules.assets.service;

import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura do imobilizado para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
@Service
@Transactional(readOnly = true)
public class AssetLookup {

    private final AssetRepository assetRepository;

    public AssetLookup(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /** Activos da empresa, ordenados pelo código. */
    public List<Asset> listForCompany(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return assetRepository.findByCompanyIdOrderByAssetCodeAsc(companyId);
    }
}
