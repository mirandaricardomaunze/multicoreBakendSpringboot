package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.CashFlowForecastApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular: CashFlowForecastPanel")
class CashFlowForecastPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("CFF-01: CashFlowForecastPanel.java deve possuir <= 230 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("CashFlowForecastPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 230,
                () -> "CashFlowForecastPanel.java deve possuir <= 230 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("CFF-02: Colaboradores desacoplados devem respeitar os limites de linhas")
    void testCollaboratorsLineCount() throws IOException {
        Path coord = ROOT.resolve("CashFlowForecastOperationsCoordinator.java");
        List<String> coordLines = Files.readAllLines(coord);
        assertTrue(coordLines.size() <= 85,
                () -> "CashFlowForecastOperationsCoordinator.java deve possuir <= 85 linhas. Atual: " + coordLines.size());

        Path details = ROOT.resolve("CashFlowForecastDetailsPanel.java");
        List<String> detailsLines = Files.readAllLines(details);
        assertTrue(detailsLines.size() <= 95,
                () -> "CashFlowForecastDetailsPanel.java deve possuir <= 95 linhas. Atual: " + detailsLines.size());

        Path renderer = ROOT.resolve("CashFlowMoneyCellRenderer.java");
        List<String> rendererLines = Files.readAllLines(renderer);
        assertTrue(rendererLines.size() <= 45,
                () -> "CashFlowMoneyCellRenderer.java deve possuir <= 45 linhas. Atual: " + rendererLines.size());
    }

    @Test
    @DisplayName("CFF-03: Contratos literais canónicos preservados")
    void testCanonicalContractsPreserved() throws IOException {
        String content = Files.readString(ROOT.resolve("CashFlowForecastPanel.java"));

        assertTrue(content.contains("new ModernPanel(16)"), "Falta new ModernPanel(16)");
        assertFalse(content.contains("new ModernPanel(10)"), "Não deve conter new ModernPanel(10)");
        assertTrue(content.contains("KpiCard.createGrid(4)"), "Falta KpiCard.createGrid(4)");
        assertTrue(content.contains("UIHelper.ensureHeadersFit(bucketsTable)"), "Falta UIHelper.ensureHeadersFit(bucketsTable)");
    }

    @Test
    @DisplayName("CFF-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("CashFlowForecastPanel.java", "CashFlowForecastOperationsCoordinator.java",
                "CashFlowForecastDetailsPanel.java", "CashFlowMoneyCellRenderer.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("CFF-05: Construtor público e métodos de contrato preservados")
    void testPublicApiPreserved() throws NoSuchMethodException {
        CashFlowForecastPanel.class.getConstructor(CashFlowForecastApiClient.class);
        CashFlowForecastPanel.class.getMethod("loadForecast");
        CashFlowForecastPanel.class.getMethod("displayForecast", mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO.class);
        CashFlowForecastPanel.class.getMethod("getBucketsTable");
        CashFlowForecastPanel.class.getMethod("getAlertBanner");
        CashFlowForecastPanel.class.getMethod("getCurrentForecast");
    }
}
