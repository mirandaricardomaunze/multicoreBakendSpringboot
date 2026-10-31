package mz.multicore.erp.modules.hr.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Decomposição Modular - EmploymentContractService (Fase 118)")
class EmploymentContractServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of("src/main/java/mz/multicore/erp/modules/hr/service/EmploymentContractService.java");
    private static final Path VALIDATION_FILE = Path.of("src/main/java/mz/multicore/erp/modules/hr/service/ContractValidationService.java");
    private static final Path LIFECYCLE_FILE = Path.of("src/main/java/mz/multicore/erp/modules/hr/service/ContractLifecycleService.java");
    private static final Path QUERY_FILE = Path.of("src/main/java/mz/multicore/erp/modules/hr/service/ContractQueryService.java");

    @Test
    @DisplayName("Garante limites de linhas para o orquestrador e colaboradores")
    void testFilePhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_FILE).count();
        long validationLines = Files.lines(VALIDATION_FILE).count();
        long lifecycleLines = Files.lines(LIFECYCLE_FILE).count();
        long queryLines = Files.lines(QUERY_FILE).count();

        assertTrue(serviceLines <= 125, "EmploymentContractService deve ter <= 125 linhas, atual: " + serviceLines);
        assertTrue(validationLines <= 110, "ContractValidationService deve ter <= 110 linhas, atual: " + validationLines);
        assertTrue(lifecycleLines <= 220, "ContractLifecycleService deve ter <= 220 linhas, atual: " + lifecycleLines);
        assertTrue(queryLines <= 140, "ContractQueryService deve ter <= 140 linhas, atual: " + queryLines);
    }

    @Test
    @DisplayName("Garante que não há uso proibido de BigDecimal.ZERO.equals")
    void testZeroBigDecimalEqualsUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(VALIDATION_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(LIFECYCLE_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(QUERY_FILE).contains("BigDecimal.ZERO.equals"));
    }

    @Test
    @DisplayName("Garante ausência de JOptionPane ou UI no backend")
    void testZeroJOptionPaneUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(VALIDATION_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(LIFECYCLE_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(QUERY_FILE).contains("JOptionPane"));
    }

    @Test
    @DisplayName("Garante delegação do orquestrador para os colaboradores")
    void testDelegationPattern() throws IOException {
        String serviceSource = Files.readString(SERVICE_FILE);
        assertTrue(serviceSource.contains("lifecycleService.createContract"));
        assertTrue(serviceSource.contains("lifecycleService.activateContract"));
        assertTrue(serviceSource.contains("lifecycleService.renewContract"));
        assertTrue(serviceSource.contains("queryService.getAllContracts"));
    }
}
