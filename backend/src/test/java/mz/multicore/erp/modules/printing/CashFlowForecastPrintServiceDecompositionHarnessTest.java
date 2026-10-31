package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashFlowForecastPrintServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/printing");

    @Test
    void forecastDocumentRenderingStaysExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("CashFlowForecastPrintService.java"));
        String renderer = Files.readString(DIR.resolve("CashFlowForecastDocumentRenderer.java"));

        assertTrue(facade.contains("documentRenderer.render(company, forecast)"));
        assertTrue(facade.contains("forecastService.generateForecastForCompany"));
        assertFalse(facade.contains("PdfDocumentBuilder.buildA4"));
        assertTrue(renderer.contains("Mapa Previsional de Fluxo de Caixa"));
        assertTrue(Files.readAllLines(DIR.resolve("CashFlowForecastPrintService.java")).size() <= 55);
        assertTrue(Files.readAllLines(DIR.resolve("CashFlowForecastDocumentRenderer.java")).size() <= 215);
    }
}
