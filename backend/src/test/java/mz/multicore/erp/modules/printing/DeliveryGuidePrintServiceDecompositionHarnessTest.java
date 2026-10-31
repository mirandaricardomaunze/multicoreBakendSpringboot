package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryGuidePrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void deliveryGuideRenderingStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("DeliveryGuidePrintService.java"));
        String renderer = Files.readString(DIR.resolve("DeliveryGuideDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.renderDocument(guide, DELIVERY_GUIDE_COPIES)"));
        assertTrue(facade.contains("CurrentUserContext.requireCompany"));
        assertFalse(facade.contains("PdfDocumentBuilder.build"));
        assertTrue(renderer.contains("LineItemsTableRenderer.build"));
        assertTrue(renderer.contains("TotalsBlockRenderer.build"));
        assertTrue(renderer.contains("LoadSummaryRenderer.build"));
        assertEquals(4, DeliveryGuidePrintService.DELIVERY_GUIDE_COPIES.size());
        assertTrue(Files.readAllLines(DIR.resolve("DeliveryGuidePrintService.java")).size() <= 60);
        assertTrue(Files.readAllLines(DIR.resolve("DeliveryGuideDocumentRenderer.java")).size() <= 210);
    }
}
