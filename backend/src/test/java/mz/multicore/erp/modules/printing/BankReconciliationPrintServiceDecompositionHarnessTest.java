package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankReconciliationPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void bankReconciliationRenderingStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("BankReconciliationPrintService.java"));
        String renderer = Files.readString(DIR.resolve("BankReconciliationDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.renderDocument(company, statement, items)"));
        assertTrue(facade.contains("reconciliationService.getStatement"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("Mapa de Reconciliação Bancária"));
        assertTrue(renderer.contains("Saldo Inicial Extracto"));
        assertTrue(Files.readAllLines(DIR.resolve("BankReconciliationPrintService.java")).size() <= 55);
        assertTrue(Files.readAllLines(DIR.resolve("BankReconciliationDocumentRenderer.java")).size() <= 195);
    }
}
