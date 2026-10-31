package mz.multicore.erp.modules.fiscal.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiscalSalesExportServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/fiscal/service");

    @Test
    void saftXmlGenerationStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("FiscalSalesExportService.java"));
        String builder = Files.readString(DIR.resolve("FiscalSalesXmlBuilder.java"));

        assertTrue(facade.contains("xmlBuilder.buildXml("));
        assertTrue(facade.contains("PermissionGuard.requireManagerOrAdmin"));
        assertFalse(facade.contains("<AuditFile>"));
        assertTrue(builder.contains("<AuditFile>"));
        assertTrue(builder.contains("<SalesInvoices>"));
        assertEquals("A &amp; B &lt;C&gt;", FiscalSalesExportService.escape("A & B <C>"));
        assertTrue(Files.readAllLines(DIR.resolve("FiscalSalesExportService.java")).size() <= 105);
        assertTrue(Files.readAllLines(DIR.resolve("FiscalSalesXmlBuilder.java")).size() <= 160);
    }
}
