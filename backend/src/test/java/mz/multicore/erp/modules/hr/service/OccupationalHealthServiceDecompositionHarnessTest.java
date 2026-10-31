package mz.multicore.erp.modules.hr.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OccupationalHealthServiceDecompositionHarnessTest {

    private static final Path SERVICE_DIR = Path.of("src", "main", "java", "mz", "multicore", "erp", "modules", "hr", "service");

    @Test
    @DisplayName("OccupationalHealthService orchestrator deve ter no máximo 120 linhas")
    void orchestratorUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("OccupationalHealthService.java"));
        assertTrue(lines <= 120, "OccupationalHealthService deve ter <= 120 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("OccupationalHealthRegistrationService deve ter no máximo 160 linhas")
    void registrationServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("OccupationalHealthRegistrationService.java"));
        assertTrue(lines <= 160, "OccupationalHealthRegistrationService deve ter <= 160 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("OccupationalHealthFinanceService deve ter no máximo 145 linhas")
    void financeServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("OccupationalHealthFinanceService.java"));
        assertTrue(lines <= 145, "OccupationalHealthFinanceService deve ter <= 145 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("OccupationalHealthQueryService deve ter no máximo 160 linhas")
    void queryServiceUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("OccupationalHealthQueryService.java"));
        assertTrue(lines <= 160, "OccupationalHealthQueryService deve ter <= 160 linhas, mas tem: " + lines);
    }

    @Test
    @DisplayName("OccupationalHealthMapper deve ter no máximo 60 linhas")
    void mapperUnderLimit() throws IOException {
        long lines = countLines(SERVICE_DIR.resolve("OccupationalHealthMapper.java"));
        assertTrue(lines <= 60, "OccupationalHealthMapper deve ter <= 60 linhas, mas tem: " + lines);
    }

    private long countLines(Path path) throws IOException {
        return Files.lines(path).count();
    }
}
