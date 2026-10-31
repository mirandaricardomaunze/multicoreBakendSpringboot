package mz.multicore.erp.modules.quality.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Quality Equipment Service Decomposition Architectural Harness")
class QualityEquipmentServiceDecompositionHarnessTest {

    private static final Path SERVICE_DIR = Path.of("src/main/java/mz/multicore/erp/modules/quality/service");

    @Test
    @DisplayName("Garante que QualityEquipmentService foi decomposto e respeita o teto de 140 linhas")
    void qualityEquipmentServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("QualityEquipmentService.java");
        assertTrue(Files.exists(path), "QualityEquipmentService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 140, "QualityEquipmentService.java deve ter <= 140 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que EquipmentCatalogService respeita o teto de 180 linhas")
    void catalogServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("EquipmentCatalogService.java");
        assertTrue(Files.exists(path), "EquipmentCatalogService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 180, "EquipmentCatalogService.java deve ter <= 180 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que WorkerEquipmentAssignmentService respeita o teto de 205 linhas")
    void assignmentServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("WorkerEquipmentAssignmentService.java");
        assertTrue(Files.exists(path), "WorkerEquipmentAssignmentService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 205, "WorkerEquipmentAssignmentService.java deve ter <= 205 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que QualityMonitoringService respeita o teto de 120 linhas")
    void monitoringServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("QualityMonitoringService.java");
        assertTrue(Files.exists(path), "QualityMonitoringService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 120, "QualityMonitoringService.java deve ter <= 120 linhas, mas tem " + lines);
    }

    private long countLines(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        return lines.size();
    }
}
