package mz.multicore.erp.gui.inventory;

import mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient;
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

@DisplayName("HARNESS-PHYSICAL-INVENTORY-PANEL-DECOMPOSITION-001: Decomposição do Painel de Inventário Físico")
class PhysicalInventoryPanelDecompositionHarnessTest {

    private InventoryPhysicalCountingApiClient apiClient;

    @BeforeEach
    void setUp() {
        apiClient = mock(InventoryPhysicalCountingApiClient.class);
    }

    @Test
    @DisplayName("ID-01: PhysicalInventoryPanel.java reduzido para <= 240 linhas")
    void testLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryPanel.java");
        }
        assertTrue(Files.exists(path), "PhysicalInventoryPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 240, "PhysicalInventoryPanel.java deve possuir <= 240 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("ID-02: Colaboradores desacoplados e dentro dos limites")
    void testCollaboratorsLineCounts() throws IOException {
        Path coordPath = Path.of("desktop/src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryOperationsCoordinator.java");
        if (!Files.exists(coordPath)) {
            coordPath = Path.of("src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryOperationsCoordinator.java");
        }
        assertTrue(Files.exists(coordPath), "PhysicalInventoryOperationsCoordinator.java deve existir");
        List<String> coordLines = Files.readAllLines(coordPath);
        assertTrue(coordLines.size() <= 160, "PhysicalInventoryOperationsCoordinator.java deve possuir <= 160 linhas. Atual: " + coordLines.size());

        Path tableHelperPath = Path.of("desktop/src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryTableHelper.java");
        if (!Files.exists(tableHelperPath)) {
            tableHelperPath = Path.of("src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryTableHelper.java");
        }
        assertTrue(Files.exists(tableHelperPath), "PhysicalInventoryTableHelper.java deve existir");
        List<String> tableLines = Files.readAllLines(tableHelperPath);
        assertTrue(tableLines.size() <= 120, "PhysicalInventoryTableHelper.java deve possuir <= 120 linhas. Atual: " + tableLines.size());

        Path viewCoordPath = Path.of("desktop/src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryViewCoordinator.java");
        if (!Files.exists(viewCoordPath)) {
            viewCoordPath = Path.of("src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryViewCoordinator.java");
        }
        assertTrue(Files.exists(viewCoordPath), "PhysicalInventoryViewCoordinator.java deve existir");
        List<String> viewLines = Files.readAllLines(viewCoordPath);
        assertTrue(viewLines.size() <= 110, "PhysicalInventoryViewCoordinator.java deve possuir <= 110 linhas. Atual: " + viewLines.size());
    }

    @Test
    @DisplayName("ID-03: Ausência total de JOptionPane em PhysicalInventoryPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "PhysicalInventoryPanel.java",
                "PhysicalInventoryOperationsCoordinator.java",
                "PhysicalInventoryTableHelper.java",
                "PhysicalInventoryViewCoordinator.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui/inventory");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui/inventory");
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
    @DisplayName("ID-04: Métodos essenciais getItemsTable() e handleBarcodeScan() preservados")
    void testRequiredMethods() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PhysicalInventoryPanel panel = new PhysicalInventoryPanel(apiClient);
            assertNotNull(panel.getItemsTable(), "getItemsTable() deve devolver a JTable");
            assertDoesNotThrow(panel::handleBarcodeScan, "handleBarcodeScan() deve executar sem excepções");
        });
    }

    @Test
    @DisplayName("ID-05: Conformidade canónica de UI e ausência de emojis Unicode")
    void testCanonicalCardStructureAndNoUnicode() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryPanel.java");
        }
        String content = Files.readString(path);

        assertTrue(content.contains("UIHelper.tableCardTop("), "Deve usar UIHelper.tableCardTop");
        assertTrue(content.contains("KpiCard.createGrid(4)"), "Deve usar KpiCard.createGrid(4)");

        assertFalse(content.matches(".*[\\u2600-\\u27BF\\uD83C-\\uDBFF\\uDC00-\\uDFFF].*"),
                "Não deve conter caracteres emoji Unicode em PhysicalInventoryPanel.java");
    }
}
