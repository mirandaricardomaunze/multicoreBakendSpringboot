package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Decomposição Modular - CreditNoteService (Fase 116)")
class CreditNoteServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of("src/main/java/mz/multicore/erp/modules/comercial/service/CreditNoteService.java");
    private static final Path CREATION_FILE = Path.of("src/main/java/mz/multicore/erp/modules/comercial/service/CreditNoteCreationService.java");
    private static final Path LIFECYCLE_FILE = Path.of("src/main/java/mz/multicore/erp/modules/comercial/service/CreditNoteLifecycleService.java");
    private static final Path QUERY_FILE = Path.of("src/main/java/mz/multicore/erp/modules/comercial/service/CreditNoteQueryService.java");

    @Test
    @DisplayName("Garante limites de linhas para o orquestrador e colaboradores")
    void testFilePhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_FILE).count();
        long creationLines = Files.lines(CREATION_FILE).count();
        long lifecycleLines = Files.lines(LIFECYCLE_FILE).count();
        long queryLines = Files.lines(QUERY_FILE).count();

        assertTrue(serviceLines <= 105, "CreditNoteService deve ter <= 105 linhas, atual: " + serviceLines);
        assertTrue(creationLines <= 175, "CreditNoteCreationService deve ter <= 175 linhas, atual: " + creationLines);
        assertTrue(lifecycleLines <= 165, "CreditNoteLifecycleService deve ter <= 165 linhas, atual: " + lifecycleLines);
        assertTrue(queryLines <= 145, "CreditNoteQueryService deve ter <= 145 linhas, atual: " + queryLines);
    }

    @Test
    @DisplayName("Garante que não há uso proibido de BigDecimal.ZERO.equals")
    void testZeroBigDecimalEqualsUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(CREATION_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(LIFECYCLE_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(QUERY_FILE).contains("BigDecimal.ZERO.equals"));
    }

    @Test
    @DisplayName("Garante ausência de JOptionPane ou UI no backend")
    void testZeroJOptionPaneUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(CREATION_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(LIFECYCLE_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(QUERY_FILE).contains("JOptionPane"));
    }

    @Test
    @DisplayName("Garante delegação do orquestrador para os colaboradores")
    void testDelegationPattern() throws IOException {
        String serviceSource = Files.readString(SERVICE_FILE);
        assertTrue(serviceSource.contains("creationService.create"));
        assertTrue(serviceSource.contains("lifecycleService.approve"));
        assertTrue(serviceSource.contains("lifecycleService.reject"));
        assertTrue(serviceSource.contains("queryService.findByCompany"));
    }
}
