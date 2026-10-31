package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductLabelPrintServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/ProductLabelPrintService.java");
    private static final Path RENDERER_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/ProductLabelDocumentRenderer.java");

    @Test
    void productLabelPrintServiceDelegatesPdfBuildingToRenderer() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String rendererSource = Files.readString(RENDERER_FILE);

        assertTrue(serviceSource.contains("documentRenderer.renderLabels("));
        assertFalse(serviceSource.contains("new Barcode128()"));

        assertTrue(rendererSource.contains("Etiquetas de Produtos e Lotes"));
        assertTrue(rendererSource.contains("Barcode128.CODE128"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int rendererLines = Files.readAllLines(RENDERER_FILE).size();

        assertTrue(serviceLines <= 75, "ProductLabelPrintService deve ter <= 75 linhas, mas tem " + serviceLines);
        assertTrue(rendererLines <= 155, "ProductLabelDocumentRenderer deve ter <= 155 linhas, mas tem " + rendererLines);
    }
}
