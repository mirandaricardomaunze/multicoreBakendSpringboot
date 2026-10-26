package mz.multicore.erp.modules.crm.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CRM Service Decomposition Architectural Harness")
class CRMServiceDecompositionHarnessTest {

    private static final Path SERVICE_DIR = Path.of("src/main/java/mz/multicore/erp/modules/crm/service");

    @Test
    @DisplayName("Garante que CRMService foi decomposto e respeita o teto de 150 linhas")
    void crmServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("CRMService.java");
        assertTrue(Files.exists(path), "CRMService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 150, "CRMService.java deve ter <= 150 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que SupportTicketLifecycleService respeita o teto de 160 linhas")
    void ticketLifecycleServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("SupportTicketLifecycleService.java");
        assertTrue(Files.exists(path), "SupportTicketLifecycleService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 160, "SupportTicketLifecycleService.java deve ter <= 160 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que WorkSheetService respeita o teto de 265 linhas")
    void workSheetServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("WorkSheetService.java");
        assertTrue(Files.exists(path), "WorkSheetService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 265, "WorkSheetService.java deve ter <= 265 linhas, mas tem " + lines);
    }

    @Test
    @DisplayName("Garante que CRMQueryService respeita o teto de 95 linhas")
    void queryServiceUnderLimit() throws IOException {
        Path path = SERVICE_DIR.resolve("CRMQueryService.java");
        assertTrue(Files.exists(path), "CRMQueryService.java deve existir");
        long lines = countLines(path);
        assertTrue(lines <= 95, "CRMQueryService.java deve ter <= 95 linhas, mas tem " + lines);
    }

    private long countLines(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        return lines.size();
    }
}
