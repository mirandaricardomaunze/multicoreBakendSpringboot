package mz.multicore.erp.modules.financeira.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashFlowForecastServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/financeira/service");

    @Test
    void dataCollectionAndProjectionStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("CashFlowForecastService.java"));
        String collector = Files.readString(DIR.resolve("CashFlowDataCollector.java"));
        String calculator = Files.readString(DIR.resolve("CashFlowProjectionCalculator.java"));

        assertTrue(facade.contains("dataCollector.collect(companyId, today)"));
        assertTrue(facade.contains("projectionCalculator.calculate("));
        assertTrue(facade.contains("PermissionGuard.requireManagerOrAdmin"));
        assertFalse(facade.contains("TreasuryAccountType.CASH"));
        assertTrue(collector.contains("determineBucket"));
        assertTrue(calculator.contains("new CashFlowForecastDTO("));
        assertTrue(Files.readAllLines(DIR.resolve("CashFlowForecastService.java")).size() <= 60);
        assertTrue(Files.readAllLines(DIR.resolve("CashFlowDataCollector.java")).size() <= 130);
        assertTrue(Files.readAllLines(DIR.resolve("CashFlowProjectionCalculator.java")).size() <= 125);
    }
}
