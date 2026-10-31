package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValuedInventoryBookPrintServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/ValuedInventoryBookPrintService.java");
    private static final Path RENDERER_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/ValuedInventoryBookDocumentRenderer.java");

    @Test
    void valuedInventoryBookPrintServiceDelegatesPdfBuildingToRenderer() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String rendererSource = Files.readString(RENDERER_FILE);

        assertTrue(serviceSource.contains("documentRenderer.renderA4("));
        assertFalse(serviceSource.contains("new PdfPTable"));

        assertTrue(rendererSource.contains("LIVRO DE INVENTÁRIO VALORIZADO DE FIM DE EXERCÍCIO"));
        assertTrue(rendererSource.contains("VALOR GLOBAL DAS EXISTÊNCIAS (PGC-NIRF)"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int rendererLines = Files.readAllLines(RENDERER_FILE).size();

        assertTrue(serviceLines <= 60, "ValuedInventoryBookPrintService deve ter <= 60 linhas, mas tem " + serviceLines);
        assertTrue(rendererLines <= 190, "ValuedInventoryBookDocumentRenderer deve ter <= 190 linhas, mas tem " + rendererLines);
    }
}
