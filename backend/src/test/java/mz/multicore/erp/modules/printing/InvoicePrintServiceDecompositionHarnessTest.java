package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoicePrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void thermalAndFooterResponsibilitiesStayExtracted() throws Exception {
        String facade = Files.readString(DIR.resolve("InvoicePrintService.java"));
        assertTrue(facade.contains("thermalRenderer.render("));
        assertTrue(facade.contains("a4Renderer.render("));
        assertFalse(facade.contains("extends PdfPageEventHelper"));
        assertFalse(facade.contains("buildReceipt(doc ->"));
        assertTrue(Files.readAllLines(DIR.resolve("InvoiceThermalRenderer.java")).size() <= 100);
        assertTrue(Files.readAllLines(DIR.resolve("InvoicePageFooter.java")).size() <= 90);
        assertTrue(Files.readAllLines(DIR.resolve("InvoicePrintService.java")).size() <= 70);
        assertTrue(Files.readAllLines(DIR.resolve("InvoiceA4Renderer.java")).size() <= 240);
    }
}
