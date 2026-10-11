package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.model.AssetCategory;
import mz.multicore.erp.modules.assets.model.AssetStatus;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetDepreciationMapPrintServiceTest {

    @Test
    void render_geraPdfValidoComAtivosEColunasFiscais() {
        AssetRepository assetRepository = mock(AssetRepository.class);
        CompanyService companyService = mock(CompanyService.class);

        Company company = new Company();
        company.setId(1L);
        company.setName("Moçambique Agro-Indústria, Lda");
        company.setTaxId("400123456");
        company.setAddress("Av. 24 de Julho, Maputo");

        Asset asset = new Asset();
        asset.setId(10L);
        asset.setCompany(company);
        asset.setAssetCode("EQP-001");
        asset.setName("Empilhadora Toyota 2.5T");
        asset.setCategory(AssetCategory.MACHINERY);
        asset.setStatus(AssetStatus.ACTIVE);
        asset.setAcquisitionDate(LocalDate.of(2024, 1, 15));
        asset.setAcquisitionCost(new BigDecimal("800000.00"));
        asset.setAnnualDepreciationRate(new BigDecimal("0.20"));
        asset.setAccumulatedDepreciation(new BigDecimal("320000.00"));
        asset.setNetBookValue(new BigDecimal("480000.00"));

        when(companyService.getCompanyById(1L)).thenReturn(company);
        when(assetRepository.findByCompanyIdOrderByAssetCodeAsc(1L)).thenReturn(List.of(asset));

        AssetDepreciationMapPrintService service = new AssetDepreciationMapPrintService(assetRepository, companyService);
        byte[] pdf = service.render(1L, 2026);

        assertNotNull(pdf);
        assertTrue(pdf.length > 500, "O PDF do Modelo 22 Anexo D deve conter bytes válidos gerados");
    }
}
