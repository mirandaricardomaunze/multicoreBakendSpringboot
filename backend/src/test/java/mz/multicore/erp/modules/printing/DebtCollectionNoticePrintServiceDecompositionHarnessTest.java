package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebtCollectionNoticePrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void debtCollectionNoticeRenderingStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("DebtCollectionNoticePrintService.java"));
        String renderer = Files.readString(DIR.resolve("DebtCollectionNoticeDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.renderDocument(company, notice)"));
        assertTrue(facade.contains("creditRiskService.getDebtCollectionNotice"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("Carta de Notificação de Cobrança"));
        assertTrue(renderer.contains("DADOS BANCÁRIOS PARA LIQUIDAÇÃO:"));
        assertTrue(Files.readAllLines(DIR.resolve("DebtCollectionNoticePrintService.java")).size() <= 55);
        assertTrue(Files.readAllLines(DIR.resolve("DebtCollectionNoticeDocumentRenderer.java")).size() <= 210);
    }
}
