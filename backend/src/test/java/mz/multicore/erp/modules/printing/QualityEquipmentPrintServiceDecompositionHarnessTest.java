package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualityEquipmentPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void qualityEquipmentTermRenderingStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("QualityEquipmentPrintService.java"));
        String renderer = Files.readString(DIR.resolve("QualityEquipmentDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.renderDocument(assignment)"));
        assertTrue(facade.contains("assignmentLookup.requireForCompany(assignmentId, effectiveCompanyId)"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("TERMO DE ENTREGA E RESPONSABILIDADE DE EPI"));
        assertTrue(Files.readAllLines(DIR.resolve("QualityEquipmentPrintService.java")).size() <= 55);
        assertTrue(Files.readAllLines(DIR.resolve("QualityEquipmentDocumentRenderer.java")).size() <= 190);
    }
}
