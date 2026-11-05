package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
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
 * Suite de testes automatizados para conformidade com HARNESS-STOCK-PANEL-DECOMPOSITION-001.
 * Valida limites de linhas, desacoplamento dos coordenadores, ausência de JOptionPane e instanciação de componentes.
 */
public class StockPanelDecompositionHarnessTest {

    private static final Path GUI_PATH = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    @DisplayName("STK-DEC-01: Todos os ficheiros desacoplados cumprem estritamente os seus tetos de linhas")
    void testFileLinesWithinLimits() throws IOException {
        assertLineCount(GUI_PATH.resolve("StockPanel.java"), 450);
        assertLineCount(GUI_PATH.resolve("StockMovementsCoordinator.java"), 200);
        assertLineCount(GUI_PATH.resolve("StockLockCoordinator.java"), 120);
        assertLineCount(GUI_PATH.resolve("StockWarehouseCoordinator.java"), 160);
    }

    @Test
    @DisplayName("STK-DEC-02: Zero chamadas a JOptionPane em todos os ficheiros do domínio de stock")
    void testZeroJOptionPaneUsage() throws IOException {
        List<String> files = List.of(
                "StockPanel.java",
                "StockMovementsCoordinator.java",
                "StockLockCoordinator.java",
                "StockWarehouseCoordinator.java"
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
    @DisplayName("STK-DEC-03: Instanciação limpa e verificação dos componentes e abas no StockPanel")
    void testStockPanelComponentWiring() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
                ComercialApiClient comApi = Mockito.mock(ComercialApiClient.class);
                StockTransferApiClient trApi = Mockito.mock(StockTransferApiClient.class);
                InventoryCountApiClient countApi = Mockito.mock(InventoryCountApiClient.class);
                ProductCategoryApiClient catApi = Mockito.mock(ProductCategoryApiClient.class);
                PrintApiClient printApi = Mockito.mock(PrintApiClient.class);
                StockWasteApiClient wasteApi = Mockito.mock(StockWasteApiClient.class);

                StockPanel panel = new StockPanel(invApi, comApi, trApi, countApi, catApi, printApi, wasteApi);
                assertNotNull(panel);
                assertNotNull(panel.stockTable);
                assertNotNull(panel.movementsTable);
                assertNotNull(panel.transferTable);
                assertNotNull(panel.kpiTransfersInTransit);
                assertNotNull(panel.kpiTransfersSeparation);
                assertNotNull(panel.kpiTransfersPending);
                assertNotNull(panel.kpiTransfersCompleted);
                assertNotNull(panel.kpiTransfersTotalValue);

                assertEquals(9, panel.stockTable.getColumnCount(), "A tabela de stock deve possuir 9 colunas (inclui Validade)");
                assertEquals(9, panel.movementsTable.getColumnCount(), "A tabela de movimentos deve possuir 9 colunas");
                assertEquals(9, panel.transferTable.getColumnCount(), "A tabela de transferências deve possuir 9 colunas");
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    @Test
    @DisplayName("STK-DEC-04: Suporte ao construtor estendido com InventoryPhysicalCountingApiClient")
    void testStockPanelExtendedConstructor() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                InventoryApiClient invApi = Mockito.mock(InventoryApiClient.class);
                ComercialApiClient comApi = Mockito.mock(ComercialApiClient.class);
                StockTransferApiClient trApi = Mockito.mock(StockTransferApiClient.class);
                InventoryCountApiClient countApi = Mockito.mock(InventoryCountApiClient.class);
                ProductCategoryApiClient catApi = Mockito.mock(ProductCategoryApiClient.class);
                PrintApiClient printApi = Mockito.mock(PrintApiClient.class);
                StockWasteApiClient wasteApi = Mockito.mock(StockWasteApiClient.class);
                InventoryPhysicalCountingApiClient physApi = Mockito.mock(InventoryPhysicalCountingApiClient.class);

                StockPanel panel = new StockPanel(invApi, comApi, trApi, countApi, catApi, printApi, wasteApi, physApi);
                assertNotNull(panel);
                assertNotNull(panel.stockTable);
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    private void assertLineCount(Path file, int maxLines) throws IOException {
        assertTrue(Files.exists(file), file + " deve existir");
        long count = Files.lines(file).count();
        assertTrue(count <= maxLines,
                String.format("Ficheiro %s excedeu o teto de %d linhas (actual: %d)", file.getFileName(), maxLines, count));
    }
}
