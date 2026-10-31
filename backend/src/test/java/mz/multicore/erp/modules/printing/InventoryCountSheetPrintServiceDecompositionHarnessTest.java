package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryCountSheetPrintServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/InventoryCountSheetPrintService.java");
    private static final Path RENDERER_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/InventoryCountSheetDocumentRenderer.java");

    @Test
    void inventoryCountSheetPrintServiceDelegatesPdfBuildingToRenderer() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String rendererSource = Files.readString(RENDERER_FILE);

        assertTrue(serviceSource.contains("documentRenderer.renderA4("));
        assertFalse(serviceSource.contains("new PdfPTable"));

        assertTrue(rendererSource.contains("Folha de Contagem de Inventário"));
        assertTrue(rendererSource.contains("Âmbito (contagem cega)"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int rendererLines = Files.readAllLines(RENDERER_FILE).size();

        assertTrue(serviceLines <= 75, "InventoryCountSheetPrintService deve ter <= 75 linhas, mas tem " + serviceLines);
        assertTrue(rendererLines <= 150, "InventoryCountSheetDocumentRenderer deve ter <= 150 linhas, mas tem " + rendererLines);
    }
}
