package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicAuditPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void forensicAuditDocumentRenderingStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("ForensicAuditPrintService.java"));
        String renderer = Files.readString(DIR.resolve("ForensicAuditDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.renderDocument(company, summary, startDate, endDate)"));
        assertTrue(facade.contains("auditService.getForensicSummaryForCompany"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("Dossiê de Auditoria Forense & Controlo Interno"));
        assertTrue(renderer.contains("CLASSIFICAÇÃO DE RISCO:"));
        assertTrue(Files.readAllLines(DIR.resolve("ForensicAuditPrintService.java")).size() <= 60);
        assertTrue(Files.readAllLines(DIR.resolve("ForensicAuditDocumentRenderer.java")).size() <= 200);
    }
}
