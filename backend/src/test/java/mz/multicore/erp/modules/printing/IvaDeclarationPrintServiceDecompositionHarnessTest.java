package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IvaDeclarationPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void ivaDeclarationRenderingStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("IvaDeclarationPrintService.java"));
        String renderer = Files.readString(DIR.resolve("IvaDeclarationDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.renderDocument(company, summary, year, month)"));
        assertTrue(facade.contains("fiscalSummaryService.computeMonth"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("Declaração Mensal de IVA"));
        assertTrue(renderer.contains("IVA A ENTREGAR AO ESTADO (A PAGAR)"));
        assertTrue(Files.readAllLines(DIR.resolve("IvaDeclarationPrintService.java")).size() <= 60);
        assertTrue(Files.readAllLines(DIR.resolve("IvaDeclarationDocumentRenderer.java")).size() <= 195);
    }
}
