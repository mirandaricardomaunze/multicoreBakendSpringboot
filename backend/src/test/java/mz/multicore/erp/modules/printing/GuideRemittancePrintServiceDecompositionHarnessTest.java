package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideRemittancePrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void remittanceGuideCompositionStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("GuideRemittancePrintService.java"));
        String renderer = Files.readString(DIR.resolve("GuideRemittanceDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.render(invoice)"));
        assertTrue(facade.contains("CurrentUserContext.requireCompany"));
        assertFalse(facade.contains("PdfDocumentBuilder.build"));
        assertTrue(renderer.contains("LineItemsTableRenderer.build"));
        assertTrue(renderer.contains("TotalsBlockRenderer.build"));
        assertTrue(renderer.contains("LoadSummaryRenderer.build"));
        assertTrue(Files.readAllLines(DIR.resolve("GuideRemittancePrintService.java")).size() <= 60);
        assertTrue(Files.readAllLines(DIR.resolve("GuideRemittanceDocumentRenderer.java")).size() <= 235);
    }
}
