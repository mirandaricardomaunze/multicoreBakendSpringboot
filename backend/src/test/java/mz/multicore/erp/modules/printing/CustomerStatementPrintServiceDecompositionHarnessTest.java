package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerStatementPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void customerStatementCompositionStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("CustomerStatementPrintService.java"));
        String renderer = Files.readString(DIR.resolve("CustomerStatementDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.render(company, statement)"));
        assertTrue(facade.contains("statementService.getStatement"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("Extrato de Conta Corrente"));
        assertTrue(renderer.contains("TERMO DE CIRCULARIZAÇÃO"));
        assertTrue(Files.readAllLines(DIR.resolve("CustomerStatementPrintService.java")).size() <= 55);
        assertTrue(Files.readAllLines(DIR.resolve("CustomerStatementDocumentRenderer.java")).size() <= 210);
    }
}
