package mz.multicore.erp.modules.financeira.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bank Reconciliation Service Decomposition Architectural Harness")
class BankReconciliationServiceDecompositionHarnessTest {

    private static final Path SERVICE_DIR = Path.of("src/main/java/mz/multicore/erp/modules/financeira/service");

    @Test
    @DisplayName("Garante que BankReconciliationService foi decomposto e respeita o teto de 110 linhas")
    void reconciliationServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("BankReconciliationService.java");
        assertTrue(Files.exists(path), "BankReconciliationService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 110, "BankReconciliationService.java deve ter <= 110 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que BankReconciliationMatcher respeita o teto de 240 linhas")
    void matcherUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("BankReconciliationMatcher.java");
        assertTrue(Files.exists(path), "BankReconciliationMatcher.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 240, "BankReconciliationMatcher.java deve ter <= 240 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que BankReconciliationQueryService respeita o teto de 175 linhas")
    void queryServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("BankReconciliationQueryService.java");
        assertTrue(Files.exists(path), "BankReconciliationQueryService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 175, "BankReconciliationQueryService.java deve ter <= 175 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que BankStatementImportService respeita o teto de 120 linhas")
    void importServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("BankStatementImportService.java");
        assertTrue(Files.exists(path), "BankStatementImportService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 120, "BankStatementImportService.java deve ter <= 120 linhas, mas tem " + lines);
    }

    private long countLines(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        return lines.size();
    }
}
