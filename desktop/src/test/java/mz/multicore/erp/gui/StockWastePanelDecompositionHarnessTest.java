package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.StockWasteApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de testes automatizados para conformidade com HARNESS-STOCK-WASTE-PANEL-DECOMPOSITION-001.
 * Valida limites de linhas, desacoplamento, ausência de JOptionPane e integridade operacional.
 */
public class StockWastePanelDecompositionHarnessTest {

    private static final Path GUI_PATH = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    @DisplayName("SWP-DEC-01: Todos os ficheiros desacoplados cumprem estritamente os seus tetos de linhas")
    void testFileLinesWithinLimits() throws IOException {
        assertLineCount(GUI_PATH.resolve("StockWastePanel.java"), 380);
        assertLineCount(GUI_PATH.resolve("StockWasteRegistrationDialog.java"), 200);
        assertLineCount(GUI_PATH.resolve("StockWasteRadarView.java"), 190);
        assertLineCount(GUI_PATH.resolve("StockWasteMetricsView.java"), 180);
    }

    @Test
    @DisplayName("SWP-DEC-02: Zero chamadas a JOptionPane em todos os ficheiros do domínio de quebras")
    void testZeroJOptionPaneUsage() throws IOException {
        List<String> files = List.of(
                "StockWastePanel.java",
                "StockWasteRegistrationDialog.java",
                "StockWasteRadarView.java",
                "StockWasteMetricsView.java"
        );

        for (String file : files) {
            Path path = GUI_PATH.resolve(file);
            assertTrue(Files.exists(path), file + " deve existir");
            List<String> lines = Files.readAllLines(path);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                assertFalse(line.contains("JOptionPane"),
                        String.format("Violação em %s na linha %d: proibido uso de JOptionPane.", file, i + 1));
            }
        }
    }

    @Test
    @DisplayName("SWP-DEC-03: Instanciação limpa e verificação dos componentes acoplados no StockWastePanel")
    void testStockWastePanelComponentWiring() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                StockWasteApiClient wasteApi = Mockito.mock(StockWasteApiClient.class);
                InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
                ComercialApiClient comApi = Mockito.mock(ComercialApiClient.class);

                StockWastePanel panel = new StockWastePanel(wasteApi, invApi, comApi);
                assertNotNull(panel);
                assertNotNull(panel.kpiTotalCost);
                assertNotNull(panel.kpiTotalQty);
                assertNotNull(panel.kpiWasteRate);
                assertNotNull(panel.kpiRiskBatches);
                assertNotNull(panel.wasteTable);
                assertNotNull(panel.radarTable);

                assertEquals(13, panel.wasteTable.getColumnCount(), "A tabela de quebras deve possuir 13 colunas");
                assertEquals(10, panel.radarTable.getColumnCount(), "A tabela de radar deve possuir 10 colunas");
                assertEquals(42, panel.wasteTable.getRowHeight(), "Altura de linha da tabela de quebras deve ser 42px");
                assertEquals(42, panel.radarTable.getRowHeight(), "Altura de linha da tabela de radar deve ser 42px");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("SWP-DEC-04: StockWasteRadarView e StockWasteMetricsView inicializam corretamente")
    void testSubviewsInitialization() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                StockWasteRadarView radarView = new StockWasteRadarView(() -> {}, alert -> {});
                assertNotNull(radarView.getRadarTable());
                assertEquals(10, radarView.getRadarTable().getColumnCount());
                assertEquals(42, radarView.getRadarTable().getRowHeight());
                assertNotNull(radarView.urgencyFilterCombo);
                assertNotNull(radarView.radarSearchField);

                StockWasteApiClient wasteApi = Mockito.mock(StockWasteApiClient.class);
                StockWasteMetricsView metricsView = new StockWasteMetricsView(wasteApi, summary -> {});
                assertNotNull(metricsView.getReasonMetricsModel());
                assertNotNull(metricsView.getCategoryMetricsModel());
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    private void assertLineCount(Path path, int maxLines) throws IOException {
        assertTrue(Files.exists(path), path.getFileName() + " deve existir");
        long count = Files.lines(path).count();
        assertTrue(count <= maxLines,
                String.format("%s tem %d linhas; limite máximo é %d", path.getFileName(), count, maxLines));
    }
}
