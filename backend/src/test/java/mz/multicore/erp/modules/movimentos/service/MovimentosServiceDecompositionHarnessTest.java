package mz.multicore.erp.modules.movimentos.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Decomposição Modular - MovimentosService (Fase 113)")
class MovimentosServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of("src/main/java/mz/multicore/erp/modules/movimentos/service/MovimentosService.java");
    private static final Path COLLECTOR_FILE = Path.of("src/main/java/mz/multicore/erp/modules/movimentos/service/MovimentoCollector.java");
    private static final Path SUMMARIZER_FILE = Path.of("src/main/java/mz/multicore/erp/modules/movimentos/service/MovimentoSummarizer.java");

    @Test
    @DisplayName("Garante limites de linhas para o orquestrador e colaboradores")
    void testFilePhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_FILE).count();
        long collectorLines = Files.lines(COLLECTOR_FILE).count();
        long summarizerLines = Files.lines(SUMMARIZER_FILE).count();

        assertTrue(serviceLines <= 100, "MovimentosService deve ter <= 100 linhas, atual: " + serviceLines);
        assertTrue(collectorLines <= 205, "MovimentoCollector deve ter <= 205 linhas, atual: " + collectorLines);
        assertTrue(summarizerLines <= 190, "MovimentoSummarizer deve ter <= 190 linhas, atual: " + summarizerLines);
    }

    @Test
    @DisplayName("Garante que não há uso proibido de BigDecimal.ZERO.equals")
    void testZeroBigDecimalEqualsUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(COLLECTOR_FILE).contains("BigDecimal.ZERO.equals"));
        assertFalse(Files.readString(SUMMARIZER_FILE).contains("BigDecimal.ZERO.equals"));
    }

    @Test
    @DisplayName("Garante ausência de JOptionPane ou UI no backend")
    void testZeroJOptionPaneUsage() throws IOException {
        assertFalse(Files.readString(SERVICE_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(COLLECTOR_FILE).contains("JOptionPane"));
        assertFalse(Files.readString(SUMMARIZER_FILE).contains("JOptionPane"));
    }

    @Test
    @DisplayName("Garante que MovimentosService delega a MovimentoCollector")
    void testDelegationPattern() throws IOException {
        String serviceSource = Files.readString(SERVICE_FILE);
        assertTrue(serviceSource.contains("collector.collectMovimentos"));
        assertTrue(serviceSource.contains("CurrentUserContext.requireCompany(companyId)"));
    }
}
