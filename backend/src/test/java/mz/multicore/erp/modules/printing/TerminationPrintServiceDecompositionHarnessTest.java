package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminationPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void terminationSettlementAndCertificateStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("TerminationPrintService.java"));
        String renderer = Files.readString(DIR.resolve("TerminationDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.renderSettlement(t)"));
        assertTrue(facade.contains("documentRenderer.renderCertificate(t)"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("Acerto Final de Contas"));
        assertTrue(renderer.contains("Certificado de Trabalho"));
        assertTrue(Files.readAllLines(DIR.resolve("TerminationPrintService.java")).size() <= 55);
        assertTrue(Files.readAllLines(DIR.resolve("TerminationDocumentRenderer.java")).size() <= 195);
    }
}
