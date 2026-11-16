package mz.multicore.erp.modules.assets.service;

import mz.multicore.erp.modules.assets.model.Asset;
import mz.multicore.erp.modules.assets.model.AssetStatus;
import mz.multicore.erp.modules.assets.repository.AssetDepreciationRecordRepository;
import mz.multicore.erp.modules.assets.repository.AssetRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A run for a month never depreciates an asset bought after that month. */
class DepreciationStartsAtAcquisitionTest {

    @Test
    void assetBoughtInJuneIsNotDepreciatedInMay() {
        Asset june = asset("VIAT-01", LocalDate.of(2026, 6, 15));
        Asset april = asset("COMP-01", LocalDate.of(2026, 4, 2));
        AssetRepository assets = mock(AssetRepository.class);
        CompanyRepository companies = mock(CompanyRepository.class);
        when(assets.findByCompanyIdOrderByAssetCodeAsc(1L)).thenReturn(List.of(april, june));
        when(companies.findById(1L)).thenReturn(Optional.of(new Company()));

        var results = new AssetDepreciationBatchService(assets, mock(AssetDepreciationRecordRepository.class),
                new mz.multicore.erp.modules.company.service.CompanyLookup(companies),
                new AssetDepreciationEngine(), mock(ApplicationEventPublisher.class)).runMonthlyDepreciation(1L, 2026, 5);

        assertThat(results).extracting(r -> r.assetCode()).containsExactly("COMP-01");
        assertThat(june.getAccumulatedDepreciation()).isEqualByComparingTo("0");
    }

    private static Asset asset(String code, LocalDate acquired) {
        Asset asset = new Asset();
        asset.setId((long) code.hashCode());
        asset.setAssetCode(code);
        asset.setStatus(AssetStatus.ACTIVE);
        asset.setAcquisitionDate(acquired);
        asset.setAcquisitionCost(new BigDecimal("120000"));
        asset.setNetBookValue(new BigDecimal("120000"));
        asset.setAnnualDepreciationRate(new BigDecimal("0.25"));
        return asset;
    }
}
