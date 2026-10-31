package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmploymentContractPrintServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/EmploymentContractPrintService.java");
    private static final Path RENDERER_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/EmploymentContractDocumentRenderer.java");

    @Test
    void employmentContractPrintServiceDelegatesPdfBuildingToRenderer() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String rendererSource = Files.readString(RENDERER_FILE);

        assertTrue(serviceSource.contains("documentRenderer.renderA4("));
        assertFalse(serviceSource.contains("new PdfPTable"));

        assertTrue(rendererSource.contains("Contrato de Trabalho — "));
        assertTrue(rendererSource.contains("Condições acordadas"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int rendererLines = Files.readAllLines(RENDERER_FILE).size();

        assertTrue(serviceLines <= 55, "EmploymentContractPrintService deve ter <= 55 linhas, mas tem " + serviceLines);
        assertTrue(rendererLines <= 185, "EmploymentContractDocumentRenderer deve ter <= 185 linhas, mas tem " + rendererLines);
    }
}
