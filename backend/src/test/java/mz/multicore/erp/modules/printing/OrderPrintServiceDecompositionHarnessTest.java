package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void a4CompositionStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("OrderPrintService.java"));
        String renderer = Files.readString(DIR.resolve("OrderA4Renderer.java"));
        assertTrue(facade.contains("a4Renderer.render(order)"));
        assertTrue(facade.contains("CurrentUserContext.requireCompany"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("LineItemsTableRenderer.buildModelTable"));
        assertTrue(renderer.contains("TotalsBlockRenderer.buildModelTotals"));
        assertTrue(renderer.contains("SignatureBlockRenderer.build"));
        assertTrue(Files.readAllLines(DIR.resolve("OrderPrintService.java")).size() <= 55);
        assertTrue(Files.readAllLines(DIR.resolve("OrderA4Renderer.java")).size() <= 260);
    }
}
