package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FleetManifestPrintServiceDecompositionHarnessTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/FleetManifestPrintService.java");
    private static final Path RENDERER_FILE = Path.of(
            "src/main/java/mz/multicore/erp/modules/printing/FleetManifestDocumentRenderer.java");

    @Test
    void fleetManifestPrintServiceDelegatesPdfBuildingToRenderer() throws Exception {
        String serviceSource = Files.readString(SERVICE_FILE);
        String rendererSource = Files.readString(RENDERER_FILE);

        assertTrue(serviceSource.contains("documentRenderer.renderA4("));
        assertFalse(serviceSource.contains("new PdfPTable"));

        assertTrue(rendererSource.contains("MANIFESTO DE CARGA & ROTA DE DISTRIBUIÇÃO"));
        assertTrue(rendererSource.contains("PARADAS / ENTREGAS"));

        int serviceLines = Files.readAllLines(SERVICE_FILE).size();
        int rendererLines = Files.readAllLines(RENDERER_FILE).size();

        assertTrue(serviceLines <= 55, "FleetManifestPrintService deve ter <= 55 linhas, mas tem " + serviceLines);
        assertTrue(rendererLines <= 195, "FleetManifestDocumentRenderer deve ter <= 195 linhas, mas tem " + rendererLines);
    }
}
