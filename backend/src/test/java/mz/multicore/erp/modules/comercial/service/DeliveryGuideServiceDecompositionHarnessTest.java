package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryGuideServiceDecompositionHarnessTest {

    private static final Path SERVICE_DIR = Path.of("src", "main", "java", "mz", "multicore", "erp", "modules", "comercial", "service");

    @Test
    @DisplayName("DeliveryGuideService orchestrator deve ter no máximo 120 linhas")
    void orchestratorUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("DeliveryGuideService.java"));
        assertTrue(lines <= 120, "DeliveryGuideService deve ter <= 120 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("DeliveryGuideCreationService deve ter no máximo 190 linhas")
    void creationServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("DeliveryGuideCreationService.java"));
        assertTrue(lines <= 190, "DeliveryGuideCreationService deve ter <= 190 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("DeliveryGuideLifecycleService deve ter no máximo 160 linhas")
    void lifecycleServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("DeliveryGuideLifecycleService.java"));
        assertTrue(lines <= 160, "DeliveryGuideLifecycleService deve ter <= 160 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("DeliveryGuideQueryService deve ter no máximo 145 linhas")
    void queryServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("DeliveryGuideQueryService.java"));
        assertTrue(lines <= 145, "DeliveryGuideQueryService deve ter <= 145 linhas, mas tem: " + lines);
    }

    private long countLines(Path path) throws IOException {
        return Files.lines(path).count();
    }
}
