package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderPickingSheetPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void orderPickingSheetRenderingStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("OrderPickingSheetPrintService.java"));
        String renderer = Files.readString(DIR.resolve("OrderPickingSheetDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.renderDocument(order, reprint)"));
        assertTrue(facade.contains("CurrentUserContext.requireCompany"));
        assertFalse(facade.contains("PdfDocumentBuilder.build"));
        assertTrue(renderer.contains("GUIA DE SEPARAÇÃO / PICKING LIST"));
        assertTrue(renderer.contains("TOTAL DE EXPEDIÇÃO:"));
        assertTrue(Files.readAllLines(DIR.resolve("OrderPickingSheetPrintService.java")).size() <= 55);
        assertTrue(Files.readAllLines(DIR.resolve("OrderPickingSheetDocumentRenderer.java")).size() <= 200);
    }
}
