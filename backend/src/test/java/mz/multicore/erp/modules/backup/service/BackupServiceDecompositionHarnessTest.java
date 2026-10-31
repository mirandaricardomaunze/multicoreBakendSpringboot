package mz.multicore.erp.modules.backup.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Decomposição Modular - BackupService (Fase 117)")
class BackupServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of("src/main/java/mz/multicore/erp/modules/backup/service/BackupService.java");
    private static final Path COLLECTOR_FILE = Path.of("src/main/java/mz/multicore/erp/modules/backup/service/BackupDataCollector.java");
    private static final Path VERIFICATION_FILE = Path.of("src/main/java/mz/multicore/erp/modules/backup/service/BackupVerificationService.java");
    private static final Path EXECUTION_FILE = Path.of("src/main/java/mz/multicore/erp/modules/backup/service/BackupExecutionService.java");

    @Test
    @DisplayName("Garante limites de linhas para o orquestrador e colaboradores")
    void testFilePhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_FILE).count();
        long collectorLines = Files.lines(COLLECTOR_FILE).count();
        long verificationLines = Files.lines(VERIFICATION_FILE).count();
        long executionLines = Files.lines(EXECUTION_FILE).count();

        assertTrue(serviceLines <= 95, "BackupService deve ter <= 95 linhas, atual: " + serviceLines);
        assertTrue(collectorLines <= 245, "BackupDataCollector deve ter <= 245 linhas, atual: " + collectorLines);
        assertTrue(verificationLines <= 105, "BackupVerificationService deve ter <= 105 linhas, atual: " + verificationLines);
        assertTrue(executionLines <= 85, "BackupExecutionService deve ter <= 85 linhas, atual: " + executionLines);
    }

    @Test
    @DisplayName("Garante que não há uso proibido de BigDecimal.ZERO.equals")
    void testZeroBigDecimalEqualsUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(COLLECTOR_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(VERIFICATION_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(EXECUTION_FILE).contains("BigDecimal.ZERO.equals"));
    }

    @Test
    @DisplayName("Garante ausência de JOptionPane ou UI no backend")
    void testZeroJOptionPaneUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(COLLECTOR_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(VERIFICATION_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(EXECUTION_FILE).contains("JOptionPane"));
    }

    @Test
    @DisplayName("Garante delegação do orquestrador para os colaboradores")
    void testDelegationPattern() throws IOException {
        String serviceSource = Files.readString(SERVICE_FILE);
        assertTrue(serviceSource.contains("dataCollector.collectBackupData"));
        assertTrue(serviceSource.contains("executionService.writeBackupFile"));
        assertTrue(serviceSource.contains("verificationService.verifyBackup"));
    }
}
