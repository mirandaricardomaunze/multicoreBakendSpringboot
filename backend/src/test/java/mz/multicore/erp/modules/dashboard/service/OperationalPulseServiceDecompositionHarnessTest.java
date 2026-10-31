package mz.multicore.erp.modules.dashboard.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalPulseServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/dashboard/service/OperationalPulseService.java");
    private static final Path INDUSTRIAL_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/dashboard/service/IndustrialAndFleetPulseCalculator.java");
    private static final Path FINANCIAL_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/dashboard/service/FinancialContractsAndAssetsPulseCalculator.java");

    @Test
    void operationalPulseServiceDelegatesToPulseCalculators() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String industrialSource = Files.readString(INDUSTRIAL_FILE);
        String financialSource = Files.readString(FINANCIAL_FILE);

        assertTrue(serviceSource.contains("industrialAndFleetCalculator.calculateManufacturingPulse("));
        assertTrue(serviceSource.contains("financialAndAssetsCalculator.calculateRecurringPulse("));
        assertFalse(serviceSource.contains("ProductionOrderStatus.PLANNED"));

        assertTrue(industrialSource.contains("ManufacturingPulse("));
        assertTrue(financialSource.contains("AssetPulse("));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int industrialLines = Files.readAllLines(INDUSTRIAL_FILE).size();
        int financialLines = Files.readAllLines(FINANCIAL_FILE).size();

        assertTrue(serviceLines <= 65, "OperationalPulseService deve ter <= 65 linhas, mas tem " + serviceLines);
        assertTrue(industrialLines <= 105, "IndustrialAndFleetPulseCalculator deve ter <= 105 linhas, mas tem " + industrialLines);
        assertTrue(financialLines <= 85, "FinancialContractsAndAssetsPulseCalculator deve ter <= 85 linhas, mas tem " + financialLines);
    }
}
