package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Mapa Oficial de Amortizações e Reintegrações da Autoridade Tributária de Moçambique (Modelo 22 Anexo D - PGC-NIRF).
 * Emissão em folha A4 horizontal (Landscape) com todas as colunas de apuramento fiscal e contabilístico.
 */
@Service
public class AssetDepreciationMapPrintService {

    private final AssetRepository assetRepository;
    private final CompanyService companyService;
    private final AssetDepreciationMapDocumentRenderer documentRenderer;

    @Autowired
    public AssetDepreciationMapPrintService(AssetRepository assetRepository,
                                            CompanyService companyService,
                                            AssetDepreciationMapDocumentRenderer documentRenderer) {
        this.assetRepository = assetRepository;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public AssetDepreciationMapPrintService(AssetRepository assetRepository, CompanyService companyService) {
        this(assetRepository, companyService, new AssetDepreciationMapDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, int fiscalYear) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        List<Asset> assets = assetRepository.findByCompanyIdOrderByAssetCodeAsc(companyId);
        return documentRenderer.renderLandscapeA4(company, fiscalYear, assets);
    }
}
