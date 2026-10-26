package mz.multicore.erp.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Stock Waste Service Decomposition Architectural Harness")
class StockWasteServiceDecompositionHarnessTest {

    private static final Path SERVICE_DIR = Path.of("src/main/java/mz/multicore/erp/modules/inventory/service");

    @Test
    @DisplayName("Garante que StockWasteService foi decomposto e respeita o teto de 135 linhas")
    void wasteServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("StockWasteService.java");
        assertTrue(Files.exists(path), "StockWasteService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 135, "StockWasteService.java deve ter <= 135 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que StockWasteLifecycleService respeita o teto de 285 linhas")
    void lifecycleServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("StockWasteLifecycleService.java");
        assertTrue(Files.exists(path), "StockWasteLifecycleService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 285, "StockWasteLifecycleService.java deve ter <= 285 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que StockWasteAnalyticsService respeita o teto de 155 linhas")
    void analyticsServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("StockWasteAnalyticsService.java");
        assertTrue(Files.exists(path), "StockWasteAnalyticsService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 155, "StockWasteAnalyticsService.java deve ter <= 155 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que StockWasteQueryService respeita o teto de 75 linhas")
    void queryServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("StockWasteQueryService.java");
        assertTrue(Files.exists(path), "StockWasteQueryService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 75, "StockWasteQueryService.java deve ter <= 75 linhas, mas tem " + lines);
    }

    private long countLines(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        return lines.size();
    }
}
