package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class POSZReportPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void zReportRenderingAndSectionsStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("POSZReportPrintService.java"));
        String renderer = Files.readString(DIR.resolve("PosZReportDocumentRenderer.java"));
        String sections = Files.readString(DIR.resolve("PosZReportSectionBuilder.java"));

        assertTrue(facade.contains("documentRenderer.render(company, z)"));
        assertTrue(facade.contains("posService.buildZReport(sessionId)"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertFalse(facade.contains("ObjectMapper"));
        assertTrue(renderer.contains("sectionBuilder.buildShiftHandoversTable"));
        assertTrue(renderer.contains("sectionBuilder.buildCashBreakdownTable"));
        assertTrue(sections.contains("CashDenominationDTO"));
        assertTrue(Files.readAllLines(DIR.resolve("POSZReportPrintService.java")).size() <= 55);
        assertTrue(Files.readAllLines(DIR.resolve("PosZReportDocumentRenderer.java")).size() <= 175);
        assertTrue(Files.readAllLines(DIR.resolve("PosZReportSectionBuilder.java")).size() <= 130);
    }
}
