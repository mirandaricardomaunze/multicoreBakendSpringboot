package mz.multicore.erp.modules.assets.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/assets/service");

    @Test
    void assetLifecycleAndDepreciationBatchStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("AssetService.java"));
        String lifecycle = Files.readString(DIR.resolve("AssetLifecycleService.java"));
        String batch = Files.readString(DIR.resolve("AssetDepreciationBatchService.java"));

        assertTrue(facade.contains("lifecycleService.createAsset(companyId, request)"));
        assertTrue(facade.contains("depreciationBatchService.runMonthlyDepreciation(companyId, year, month)"));
        assertFalse(facade.contains("AssetDepreciatedEvent"));
        assertTrue(lifecycle.contains("existsByCompanyIdAndAssetCode"));
        assertTrue(batch.contains("AssetDepreciatedEvent"));
        assertTrue(Files.readAllLines(DIR.resolve("AssetService.java")).size() <= 80);
        assertTrue(Files.readAllLines(DIR.resolve("AssetLifecycleService.java")).size() <= 125);
        assertTrue(Files.readAllLines(DIR.resolve("AssetDepreciationBatchService.java")).size() <= 125);
    }
}
