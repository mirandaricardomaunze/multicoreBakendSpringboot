package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.service.AssetLookup;
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

    private final AssetLookup assetLookup;
    private final CompanyService companyService;
    private final AssetDepreciationMapDocumentRenderer documentRenderer;

    @Autowired
    public AssetDepreciationMapPrintService(AssetLookup assetLookup,
                                            CompanyService companyService,
                                            AssetDepreciationMapDocumentRenderer documentRenderer) {
        this.assetLookup = assetLookup;
        this.companyService = companyService;
        this.documentRenderer = documentRenderer;
    }

    public AssetDepreciationMapPrintService(AssetLookup assetLookup, CompanyService companyService) {
        this(assetLookup, companyService, new AssetDepreciationMapDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, int fiscalYear) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        List<Asset> assets = assetLookup.listForCompany(companyId);
        return documentRenderer.renderLandscapeA4(company, fiscalYear, assets);
    }
}
