package mz.multicore.erp.modules.assets.service;

import mz.multicore.erp.architecture.SourceLines;

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
        assertTrue(SourceLines.code(DIR.resolve("AssetService.java")) <= 80);
        assertTrue(SourceLines.code(DIR.resolve("AssetLifecycleService.java")) <= 125);
        assertTrue(SourceLines.code(DIR.resolve("AssetDepreciationBatchService.java")) <= 125);
    }
}
