package mz.multicore.erp.modules.reports.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/reports/service");

    @Test
    void analyticsAndOperationalCollectionStayExtracted() throws Exception {
        String facade = Files.readString(DIR.resolve("ReportService.java"));
        assertTrue(facade.contains("salesAnalyzer.filterRealisedSales"));
        assertTrue(facade.contains("operationsCollector.paymentSummary"));
        assertTrue(facade.contains("CurrentUserContext.requireCompany"));
        assertFalse(facade.contains("ProductMarginAccumulator"));
        assertFalse(facade.contains("Collectors.groupingBy"));
        assertTrue(Files.readAllLines(DIR.resolve("ReportService.java")).size() <= 100);
        assertTrue(Files.readAllLines(DIR.resolve("ReportSalesAnalyzer.java")).size() <= 150);
        assertTrue(Files.readAllLines(DIR.resolve("ReportOperationsCollector.java")).size() <= 140);
    }
}
