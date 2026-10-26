package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.FleetApiClient;
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

@DisplayName("HARNESS-FLEET-PANEL-DECOMPOSITION-001: Decomposição do Painel de Frotas")
class FleetPanelDecompositionHarnessTest {

    private FleetApiClient fleetApiClient;

    @BeforeEach
    void setUp() {
        fleetApiClient = mock(FleetApiClient.class);
    }

    @Test
    @DisplayName("FD-01: FleetPanel.java reduzido para <= 240 linhas")
    void testLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/FleetPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/FleetPanel.java");
        }
        assertTrue(Files.exists(path), "FleetPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 240, "FleetPanel.java deve possuir <= 240 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("FD-02: VehicleFormDialog e FuelOperationsCoordinator desacoplados e dentro dos limites")
    void testCollaboratorsLineCounts() throws IOException {
        Path vehicleDialogPath = Path.of("desktop/src/main/java/mz/multicore/erp/gui/VehicleFormDialog.java");
        if (!Files.exists(vehicleDialogPath)) {
            vehicleDialogPath = Path.of("src/main/java/mz/multicore/erp/gui/VehicleFormDialog.java");
        }
        assertTrue(Files.exists(vehicleDialogPath), "VehicleFormDialog.java deve existir");

        List<String> vehicleLines = Files.readAllLines(vehicleDialogPath);
        assertTrue(vehicleLines.size() <= 135, "VehicleFormDialog.java deve possuir <= 135 linhas. Atual: " + vehicleLines.size());

        Path fuelCoordPath = Path.of("desktop/src/main/java/mz/multicore/erp/gui/FuelOperationsCoordinator.java");
        if (!Files.exists(fuelCoordPath)) {
            fuelCoordPath = Path.of("src/main/java/mz/multicore/erp/gui/FuelOperationsCoordinator.java");
        }
        assertTrue(Files.exists(fuelCoordPath), "FuelOperationsCoordinator.java deve existir");

        List<String> fuelLines = Files.readAllLines(fuelCoordPath);
        assertTrue(fuelLines.size() <= 180, "FuelOperationsCoordinator.java deve possuir <= 180 linhas. Atual: " + fuelLines.size());
    }

    @Test
    @DisplayName("FD-03: Ausência total de JOptionPane em FleetPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "FleetPanel.java",
                "VehicleFormDialog.java",
                "FuelOperationsCoordinator.java"
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
    @DisplayName("FD-04: Campos package-private table, feedback e método showFuelHistory() preservados")
    void testRequiredFieldsAndMethods() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FleetPanel panel = new FleetPanel(fleetApiClient);
            assertNotNull(panel.table, "table package-private deve existir");
            assertNotNull(panel.feedback, "feedback package-private deve existir");
            assertDoesNotThrow(panel::showFuelHistory, "showFuelHistory() deve executar sem excepções");
        });
    }

    @Test
    @DisplayName("FD-05: Conformidade canónica de UI e ausência de emojis Unicode")
    void testCanonicalCardStructureAndNoUnicode() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/FleetPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/FleetPanel.java");
        }
        String content = Files.readString(path);

        assertTrue(content.contains("UIHelper.tableCardTop("), "Deve usar UIHelper.tableCardTop");
        assertTrue(content.contains("KpiCard.createGrid(4)"), "Deve usar KpiCard.createGrid(4)");

        assertFalse(content.matches(".*[\\u2600-\\u27BF\\uD83C-\\uDBFF\\uDC00-\\uDFFF].*"),
                "Não deve conter caracteres emoji Unicode em FleetPanel.java");
    }
}
