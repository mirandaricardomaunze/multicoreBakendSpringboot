package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-HR-OVERVIEW-TAB-PANEL-DECOMPOSITION-001: Decomposição Modular da Visão Geral de RH")
class HROverviewTabPanelDecompositionHarnessTest {

    @Test
    @DisplayName("HOT-01: HROverviewTabPanel.java reduzido para <= 110 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/HROverviewTabPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/HROverviewTabPanel.java");
        }
        assertTrue(Files.exists(path), "HROverviewTabPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 110, "HROverviewTabPanel.java deve possuir <= 110 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("HOT-02: HROverviewMetricsCoordinator.java desacoplado com <= 115 linhas")
    void testCollaboratorLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/HROverviewMetricsCoordinator.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/HROverviewMetricsCoordinator.java");
        }
        assertTrue(Files.exists(path), "HROverviewMetricsCoordinator.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 115, "HROverviewMetricsCoordinator.java deve possuir <= 115 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("HOT-03: Erradicação absoluta de JOptionPane em HROverviewTabPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "HROverviewTabPanel.java",
                "HROverviewMetricsCoordinator.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> lines = Files.readAllLines(filePath);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                assertFalse(line.contains("JOptionPane"),
                        "Uso proibido de JOptionPane em " + file + " na linha " + (i + 1) + ": " + line);
            }
        }
    }

    @Test
    @DisplayName("HOT-04: Contrato de construtor e métodos preservados")
    void testConstructorAndMethods() {
        assertDoesNotThrow(() ->
                HROverviewTabPanel.class.getDeclaredConstructor(HRPanel.class),
                "HROverviewTabPanel deve possuir construtor com HRPanel");

        assertDoesNotThrow(() ->
                HROverviewTabPanel.class.getDeclaredMethod("buildPanel"),
                "HROverviewTabPanel deve possuir método buildPanel()");

        assertDoesNotThrow(() ->
                HROverviewTabPanel.class.getDeclaredMethod("refresh"),
                "HROverviewTabPanel deve possuir método refresh()");
    }
}
