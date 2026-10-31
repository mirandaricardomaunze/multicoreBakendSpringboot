package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierStatementPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void supplierStatementRenderingStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("SupplierStatementPrintService.java"));
        String renderer = Files.readString(DIR.resolve("SupplierStatementDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.renderDocument(company, statement)"));
        assertTrue(facade.contains("statementService.getStatement"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("Extrato de Conta Corrente — Fornecedor"));
        assertTrue(renderer.contains("TERMO DE CIRCULARIZAÇÃO E CONFERÊNCIA DE CONTAS A PAGAR:"));
        assertTrue(Files.readAllLines(DIR.resolve("SupplierStatementPrintService.java")).size() <= 55);
        assertTrue(Files.readAllLines(DIR.resolve("SupplierStatementDocumentRenderer.java")).size() <= 195);
    }
}
