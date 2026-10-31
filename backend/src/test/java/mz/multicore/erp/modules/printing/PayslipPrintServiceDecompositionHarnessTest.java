package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PayslipPrintServiceDecompositionHarnessTest {

    private static final Path SERVICE_DIR = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void facadeRemainsSmallAndDelegatesPdfComposition() throws Exception {
        String source = Files.readString(SERVICE_DIR.resolve("PayslipPrintService.java"));
        assertTrue(source.lines().count() <= 60, "PayslipPrintService deve ter no máximo 60 linhas");
        assertTrue(source.contains("PayslipDocumentRenderer"));
        assertTrue(source.contains("documentRenderer.render("));
        assertTrue(Files.exists(SERVICE_DIR.resolve("PayslipCompensationTableRenderer.java")));
        assertTrue(Files.exists(SERVICE_DIR.resolve("PayslipDocumentRenderer.java")));
    }

    @Test
    void collaboratorsKeepSingleResponsibilities() throws Exception {
        String document = Files.readString(SERVICE_DIR.resolve("PayslipDocumentRenderer.java"));
        String compensation = Files.readString(SERVICE_DIR.resolve("PayslipCompensationTableRenderer.java"));
        assertTrue(document.lines().count() <= 220, "PayslipDocumentRenderer deve ter no máximo 220 linhas");
        assertTrue(compensation.lines().count() <= 150,
                "PayslipCompensationTableRenderer deve ter no máximo 150 linhas");
        assertTrue(compensation.contains("deductionService.linesOf("));
        assertTrue(!document.contains("PayrollDeductionService"));
    }
}
