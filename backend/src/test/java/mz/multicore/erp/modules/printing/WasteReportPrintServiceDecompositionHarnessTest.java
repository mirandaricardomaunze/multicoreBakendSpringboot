package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WasteReportPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void wasteReportCompositionStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("WasteReportPrintService.java"));
        String renderer = Files.readString(DIR.resolve("WasteReportDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.render(company, effectiveStart, effectiveEnd, records, summary)"));
        assertTrue(facade.contains("wasteService.getSummary"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("Relatório de Quebras e Perdas de Stock"));
        assertTrue(Files.readAllLines(DIR.resolve("WasteReportPrintService.java")).size() <= 70);
        assertTrue(Files.readAllLines(DIR.resolve("WasteReportDocumentRenderer.java")).size() <= 200);
    }
}
