package mz.multicore.erp.modules.hr.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminationServiceDecompositionHarnessTest {

    private static final Path SERVICE_DIR = Path.of("src", "main", "java", "mz", "multicore", "erp", "modules", "hr", "service");

    @Test
    @DisplayName("TerminationService orchestrator deve ter no máximo 125 linhas")
    void orchestratorUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("TerminationService.java"));
        assertTrue(lines <= 125, "TerminationService deve ter <= 125 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("TerminationSettlementCalculator deve ter no máximo 210 linhas")
    void calculatorUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("TerminationSettlementCalculator.java"));
        assertTrue(lines <= 210, "TerminationSettlementCalculator deve ter <= 210 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("TerminationLifecycleService deve ter no máximo 145 linhas")
    void lifecycleServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("TerminationLifecycleService.java"));
        assertTrue(lines <= 145, "TerminationLifecycleService deve ter <= 145 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("TerminationQueryService deve ter no máximo 100 linhas")
    void queryServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("TerminationQueryService.java"));
        assertTrue(lines <= 100, "TerminationQueryService deve ter <= 100 linhas, mas tem: " + lines);
    }

    private long countLines(Path path) throws IOException {
        return Files.lines(path).count();
    }
}
