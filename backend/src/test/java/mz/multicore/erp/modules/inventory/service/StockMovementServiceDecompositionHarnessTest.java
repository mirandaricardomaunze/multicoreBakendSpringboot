package mz.multicore.erp.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Decomposição Modular - StockMovementService (Fase 114)")
class StockMovementServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of("src/main/java/mz/multicore/erp/modules/inventory/service/StockMovementService.java");
    private static final Path REGISTRATION_FILE = Path.of("src/main/java/mz/multicore/erp/modules/inventory/service/StockMovementRegistrationService.java");
    private static final Path ADJUSTMENT_FILE = Path.of("src/main/java/mz/multicore/erp/modules/inventory/service/StockAdjustmentCoordinator.java");
    private static final Path QUERY_FILE = Path.of("src/main/java/mz/multicore/erp/modules/inventory/service/StockMovementQueryService.java");

    @Test
    @DisplayName("Garante limites de linhas para o orquestrador e colaboradores")
    void testFilePhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_FILE).count();
        long registrationLines = Files.lines(REGISTRATION_FILE).count();
        long adjustmentLines = Files.lines(ADJUSTMENT_FILE).count();
        long queryLines = Files.lines(QUERY_FILE).count();

        assertTrue(serviceLines <= 140, "StockMovementService deve ter <= 140 linhas, atual: " + serviceLines);
        assertTrue(registrationLines <= 230, "StockMovementRegistrationService deve ter <= 230 linhas, atual: " + registrationLines);
        assertTrue(adjustmentLines <= 165, "StockAdjustmentCoordinator deve ter <= 165 linhas, atual: " + adjustmentLines);
        assertTrue(queryLines <= 95, "StockMovementQueryService deve ter <= 95 linhas, atual: " + queryLines);
    }

    @Test
    @DisplayName("Garante que não há uso proibido de BigDecimal.ZERO.equals")
    void testZeroBigDecimalEqualsUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(REGISTRATION_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(ADJUSTMENT_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(QUERY_FILE).contains("BigDecimal.ZERO.equals"));
    }

    @Test
    @DisplayName("Garante ausência de JOptionPane ou UI no backend")
    void testZeroJOptionPaneUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(REGISTRATION_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(ADJUSTMENT_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(QUERY_FILE).contains("JOptionPane"));
    }

    @Test
    @DisplayName("Garante delegação do orquestrador para os colaboradores")
    void testDelegationPattern() throws IOException {
        String serviceSource = Files.readString(SERVICE_FILE);
        assertTrue(serviceSource.contains("registrationService.registerMovement"));
        assertTrue(serviceSource.contains("adjustmentCoordinator.adjustStock"));
        assertTrue(serviceSource.contains("queryService.findMovementsByCompany"));
    }
}
