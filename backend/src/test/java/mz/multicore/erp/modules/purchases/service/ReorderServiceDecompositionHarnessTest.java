package mz.multicore.erp.modules.purchases.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReorderServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/purchases/service/ReorderService.java");
    private static final Path COLLECTOR_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/purchases/service/ReorderDataCollector.java");
    private static final Path CALCULATOR_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/purchases/service/ReorderSuggestionCalculator.java");

    @Test
    void reorderServiceDelegatesToCollectorAndCalculator() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String collectorSource = Files.readString(COLLECTOR_FILE);
        String calculatorSource = Files.readString(CALCULATOR_FILE);

        assertTrue(serviceSource.contains("suggestionCalculator.computeSuggestions("));
        assertTrue(serviceSource.contains("dataCollector.collectStockByProduct("));
        assertFalse(serviceSource.contains("new ReorderSuggestionDTO("));

        assertTrue(collectorSource.contains("sumQuantitySoldByProductSince("));
        assertTrue(calculatorSource.contains("ESGOTADO"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int collectorLines = Files.readAllLines(COLLECTOR_FILE).size();
        int calculatorLines = Files.readAllLines(CALCULATOR_FILE).size();

        assertTrue(serviceLines <= 65, "ReorderService deve ter <= 65 linhas, mas tem " + serviceLines);
        assertTrue(collectorLines <= 85, "ReorderDataCollector deve ter <= 85 linhas, mas tem " + collectorLines);
        assertTrue(calculatorLines <= 125, "ReorderSuggestionCalculator deve ter <= 125 linhas, mas tem " + calculatorLines);
    }
}
