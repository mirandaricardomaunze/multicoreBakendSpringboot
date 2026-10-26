package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Credit Risk Service Decomposition Architectural Harness")
class CreditRiskServiceDecompositionHarnessTest {

    private static final Path SERVICE_DIR = Path.of("src/main/java/mz/multicore/erp/modules/comercial/service");

    @Test
    @DisplayName("Garante que CreditRiskService foi decomposto e respeita o teto de 120 linhas")
    void creditRiskServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("CreditRiskService.java");
        assertTrue(Files.exists(path), "CreditRiskService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 120, "CreditRiskService.java deve ter <= 120 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que CreditRiskAssessmentService respeita o teto de 255 linhas")
    void assessmentServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("CreditRiskAssessmentService.java");
        assertTrue(Files.exists(path), "CreditRiskAssessmentService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 255, "CreditRiskAssessmentService.java deve ter <= 255 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que CreditControlService respeita o teto de 150 linhas")
    void controlServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("CreditControlService.java");
        assertTrue(Files.exists(path), "CreditControlService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 150, "CreditControlService.java deve ter <= 150 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que DebtCollectionNoticeService respeita o teto de 135 linhas")
    void noticeServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("DebtCollectionNoticeService.java");
        assertTrue(Files.exists(path), "DebtCollectionNoticeService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 135, "DebtCollectionNoticeService.java deve ter <= 135 linhas, mas tem " + lines);
    }

    private long countLines(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        return lines.size();
    }
}
