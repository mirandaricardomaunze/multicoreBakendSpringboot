package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("HARNESS-MANUFACTURING-PANEL-DECOMPOSITION-001: Decomposição do Painel de Produção")
class ManufacturingPanelDecompositionHarnessTest {

    private ProductionApiClient productionApiClient;
    private ComercialApiClient comercialApiClient;
    private InventoryApiClient inventoryApiClient;

    @BeforeEach
    void setUp() {
        productionApiClient = mock(ProductionApiClient.class);
        comercialApiClient = mock(ComercialApiClient.class);
        inventoryApiClient = mock(InventoryApiClient.class);
    }

    @Test
    @DisplayName("MD-01: ManufacturingPanel.java reduzido para <= 260 linhas")
    void testLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/ManufacturingPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/ManufacturingPanel.java");
        }
        assertTrue(Files.exists(path), "ManufacturingPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 260, "ManufacturingPanel.java deve possuir <= 260 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("MD-02: ProductionOrderOperationsCoordinator e BomFormDialog desacoplados e com tamanhos adequados")
    void testCollaboratorsDecoupled() throws IOException {
        Path coordPath = Path.of("desktop/src/main/java/mz/multicore/erp/gui/ProductionOrderOperationsCoordinator.java");
        if (!Files.exists(coordPath)) {
            coordPath = Path.of("src/main/java/mz/multicore/erp/gui/ProductionOrderOperationsCoordinator.java");
        }
        assertTrue(Files.exists(coordPath), "ProductionOrderOperationsCoordinator.java deve existir");

        List<String> coordLines = Files.readAllLines(coordPath);
        assertTrue(coordLines.size() <= 260, "ProductionOrderOperationsCoordinator.java deve possuir <= 260 linhas. Atual: " + coordLines.size());

        Path bomPath = Path.of("desktop/src/main/java/mz/multicore/erp/gui/BomFormDialog.java");
        if (!Files.exists(bomPath)) {
            bomPath = Path.of("src/main/java/mz/multicore/erp/gui/BomFormDialog.java");
        }
        assertTrue(Files.exists(bomPath), "BomFormDialog.java deve existir");

        List<String> bomLines = Files.readAllLines(bomPath);
        assertTrue(bomLines.size() <= 130, "BomFormDialog.java deve possuir <= 130 linhas. Atual: " + bomLines.size());
    }

    @Test
    @DisplayName("MD-03: Erradicação absoluta de JOptionPane em ManufacturingPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "ManufacturingPanel.java",
                "ProductionOrderOperationsCoordinator.java",
                "BomFormDialog.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> content = Files.readAllLines(filePath);
            for (int i = 0; i < content.size(); i++) {
                assertFalse(content.get(i).contains("JOptionPane"),
                        file + " contém JOptionPane na linha " + (i + 1));
            }
        }
    }

    @Test
    @DisplayName("MD-04: Construtor canónico e campos essenciais preservados")
    void testConstructorsAndRequiredFields() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ManufacturingPanel panel = new ManufacturingPanel(productionApiClient, comercialApiClient, inventoryApiClient);
            assertNotNull(panel, "ManufacturingPanel deve instanciar com 3 argumentos");
            assertNotNull(panel.ordersTable, "ordersTable package-private deve existir para testes");
            assertNotNull(panel.feedback, "feedback package-private deve existir para testes");
        });
    }

    @Test
    @DisplayName("MD-05: Conformidade canónica de topo de card e ausência de emojis Unicode")
    void testCanonicalCardStructureAndNoUnicodeEmojis() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/ManufacturingPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/ManufacturingPanel.java");
        }
        String content = Files.readString(path);

        assertTrue(content.contains("UIHelper.tableCardTop("), "Deve usar UIHelper.tableCardTop");
        assertTrue(content.contains("TableFilter.bar("), "Deve usar TableFilter.bar");
        assertTrue(content.contains("KpiCard.createGrid(4)"), "Deve usar KpiCard.createGrid(4)");

        // Proibido emojis Unicode
        assertFalse(content.matches(".*[\\u2600-\\u27BF\\uD83C-\\uDBFF\\uDC00-\\uDFFF].*"),
                "Não deve conter caracteres emoji Unicode em ManufacturingPanel.java");
    }
}
