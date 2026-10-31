package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetDepreciationMapPrintServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/AssetDepreciationMapPrintService.java");
    private static final Path RENDERER_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/AssetDepreciationMapDocumentRenderer.java");

    @Test
    void assetDepreciationMapPrintServiceDelegatesPdfBuildingToRenderer() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String rendererSource = Files.readString(RENDERER_FILE);

        assertTrue(serviceSource.contains("documentRenderer.renderLandscapeA4("));
        assertFalse(serviceSource.contains("new PdfPTable"));

        assertTrue(rendererSource.contains("MODELO 22 — ANEXO D: MAPA OFICIAL DE AMORTIZAÇÕES E REINTEGRAÇÕES"));
        assertTrue(rendererSource.contains("TOTAIS GERAIS (MZN)"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int rendererLines = Files.readAllLines(RENDERER_FILE).size();

        assertTrue(serviceLines <= 55, "AssetDepreciationMapPrintService deve ter <= 55 linhas, mas tem " + serviceLines);
        assertTrue(rendererLines <= 185, "AssetDepreciationMapDocumentRenderer deve ter <= 185 linhas, mas tem " + rendererLines);
    }
}
