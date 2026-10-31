package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void posReceiptRenderingStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("ReceiptPrintService.java"));
        String renderer = Files.readString(DIR.resolve("ReceiptDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.renderDocument(invoice, cols, entries)"));
        assertTrue(facade.contains("CurrentUserContext.requireCompany"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildReceipt"));
        assertTrue(renderer.contains("RECIBO POS"));
        assertEquals("IVA: 16%", ReceiptPrintService.vatRateLabel(new BigDecimal("0.16")));
        assertEquals("IVA: Isento", ReceiptPrintService.vatRateLabel(BigDecimal.ZERO));
        assertTrue(Files.readAllLines(DIR.resolve("ReceiptPrintService.java")).size() <= 70);
        assertTrue(Files.readAllLines(DIR.resolve("ReceiptDocumentRenderer.java")).size() <= 190);
    }
}
