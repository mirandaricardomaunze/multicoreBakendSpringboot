package mz.multicore.erp.modules.manufacturing.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Decomposição Modular - ProductionService (Fase 115)")
class ProductionServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of("src/main/java/mz/multicore/erp/modules/manufacturing/service/ProductionService.java");
    private static final Path BOM_FILE = Path.of("src/main/java/mz/multicore/erp/modules/manufacturing/service/BillOfMaterialsService.java");
    private static final Path ORDER_FILE = Path.of("src/main/java/mz/multicore/erp/modules/manufacturing/service/ProductionOrderLifecycleService.java");
    private static final Path MAPPER_FILE = Path.of("src/main/java/mz/multicore/erp/modules/manufacturing/service/ProductionOrderMapper.java");

    @Test
    @DisplayName("Garante limites de linhas para o orquestrador e colaboradores")
    void testFilePhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_FILE).count();
        long bomLines = Files.lines(BOM_FILE).count();
        long orderLines = Files.lines(ORDER_FILE).count();
        long mapperLines = Files.lines(MAPPER_FILE).count();

        assertTrue(serviceLines <= 110, "ProductionService deve ter <= 110 linhas, atual: " + serviceLines);
        assertTrue(bomLines <= 165, "BillOfMaterialsService deve ter <= 165 linhas, atual: " + bomLines);
        assertTrue(orderLines <= 245, "ProductionOrderLifecycleService deve ter <= 245 linhas, atual: " + orderLines);
        assertTrue(mapperLines <= 60, "ProductionOrderMapper deve ter <= 60 linhas, atual: " + mapperLines);
    }

    @Test
    @DisplayName("Garante que não há uso proibido de BigDecimal.ZERO.equals")
    void testZeroBigDecimalEqualsUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(BOM_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(ORDER_FILE).contains("BigDecimal.ZERO.equals"));
    }

    @Test
    @DisplayName("Garante ausência de JOptionPane ou UI no backend")
    void testZeroJOptionPaneUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(BOM_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(ORDER_FILE).contains("JOptionPane"));
    }

    @Test
    @DisplayName("Garante que ProductionService delega para os colaboradores")
    void testDelegationPattern() throws IOException {
        String serviceSource = Files.readString(SERVICE_FILE);
        assertTrue(serviceSource.contains("bomService.createBom"));
        assertTrue(serviceSource.contains("orderService.createProductionOrder"));
        assertTrue(serviceSource.contains("orderService.completeProduction"));
    }
}
