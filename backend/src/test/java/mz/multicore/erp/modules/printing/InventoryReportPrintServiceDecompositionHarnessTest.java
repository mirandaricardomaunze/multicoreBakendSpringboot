package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryReportPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void inventoryReportCompositionStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("InventoryReportPrintService.java"));
        String renderer = Files.readString(DIR.resolve("InventoryReportDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.render(company, warehouseFilter, mutableStocks)"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("Inventário de Stock"));
        assertTrue(renderer.contains("VALOR TOTAL DO STOCK"));
        assertTrue(Files.readAllLines(DIR.resolve("InventoryReportPrintService.java")).size() <= 75);
        assertTrue(Files.readAllLines(DIR.resolve("InventoryReportDocumentRenderer.java")).size() <= 200);
    }
}
